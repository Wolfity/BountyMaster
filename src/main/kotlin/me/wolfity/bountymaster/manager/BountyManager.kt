package me.wolfity.bountymaster.manager

import me.wolfity.bountymaster.db.Bounties
import me.wolfity.developmentutil.util.launchAsync
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll

class BountyManager {

    // For now I only want it so you can place 1 bounty, and receive 1 bounty
    // Making this a list, because I might want to change that in the future
    private val activeBountiesCache = ConcurrentHashMap<Long, Bounty>()
    private val bountiesByCreator = ConcurrentHashMap<UUID, MutableList<Bounty>>()
    private val bountiesByTarget = ConcurrentHashMap<UUID, MutableList<Bounty>>()

    init {
        launchAsync {
            loadActiveBounties()
        }
    }

    suspend fun loadActiveBounties() {
        newSuspendedTransaction {
            Bounties.selectAll().forEach { row ->
                val bounty = Bounty(
                    id = row[Bounties.id],
                    creator = row[Bounties.creator],
                    target = row[Bounties.target],
                    createdAt = row[Bounties.createdAt],
                    duration = row[Bounties.duration],
                    reward = row[Bounties.reward],
                    completedAt = row[Bounties.completedAt],
                    completer = row[Bounties.completer]
                )
                if (!bounty.isCompleted()) {
                    cacheBounty(bounty)
                }
            }
        }
    }

    private fun cacheBounty(bounty: Bounty) {
        activeBountiesCache[bounty.id] = bounty

        bountiesByCreator.computeIfAbsent(bounty.creator) { Collections.synchronizedList(mutableListOf()) }
            .add(bounty)

        bountiesByTarget.computeIfAbsent(bounty.target) { Collections.synchronizedList(mutableListOf()) }
            .add(bounty)
    }

    private fun removeBountyFromCache(bounty: Bounty) {
        activeBountiesCache.remove(bounty.id)

        bountiesByCreator[bounty.creator]?.remove(bounty)
        if (bountiesByCreator[bounty.creator]?.isEmpty() == true) {
            bountiesByCreator.remove(bounty.creator)
        }

        bountiesByTarget[bounty.target]?.remove(bounty)
        if (bountiesByTarget[bounty.target]?.isEmpty() == true) {
            bountiesByTarget.remove(bounty.target)
        }
    }

    suspend fun hasHadBountyInTimeWindow(target: UUID, timeWindowMillis: Long): Boolean = newSuspendedTransaction {
        if (hasRecentActiveBountyOnTarget(target, timeWindowMillis)) {
            return@newSuspendedTransaction true
        }

        val cutoff = System.currentTimeMillis() - timeWindowMillis

        val completedExists = Bounties
            .selectAll().where {
                (Bounties.target eq target) and
                        (Bounties.createdAt greaterEq cutoff)
            }
            .limit(1)
            .any()

        completedExists
    }

    suspend fun hasPlayerPlacedBountyRecently(creator: UUID, timeWindowMillis: Long): Boolean =
        newSuspendedTransaction {
            if (hasPlayerPlacedActiveBountyRecently(creator, timeWindowMillis)) {
                return@newSuspendedTransaction true
            }

            val cutoff = System.currentTimeMillis() - timeWindowMillis

            val completedExists = Bounties
                .selectAll().where {
                    (Bounties.creator eq creator) and
                            (Bounties.createdAt greaterEq cutoff)
                }
                .limit(1)
                .any()

            completedExists
        }


    suspend fun startBounty(
        creator: UUID,
        target: UUID,
        durationMillis: Long,
        reward: Int
    ): Bounty = newSuspendedTransaction {
        val now = System.currentTimeMillis()
        val newId = Bounties.insert {
            it[Bounties.creator] = creator
            it[Bounties.target] = target
            it[createdAt] = now
            it[duration] = durationMillis
            it[Bounties.reward] = reward
        } get Bounties.id

        val bounty = Bounty(newId, creator, target, now, durationMillis, reward)
        cacheBounty(bounty)
        bounty
    }

    suspend fun cancelBounty(bountyId: Long): Boolean = newSuspendedTransaction {
        val bounty = activeBountiesCache[bountyId] ?: return@newSuspendedTransaction false
        val removedFromDb = Bounties.deleteWhere { id eq bountyId } > 0
        if (removedFromDb) {
            removeBountyFromCache(bounty)
        }
        removedFromDb
    }

    suspend fun completeBounty(bountyId: Long, completer: UUID): Boolean = newSuspendedTransaction {
        val bounty = activeBountiesCache[bountyId] ?: return@newSuspendedTransaction false
        val now = System.currentTimeMillis()

        val updated = Bounties.update({ Bounties.id eq bountyId }) {
            it[completedAt] = now
            it[Bounties.completer] = completer
        } > 0

        if (updated) {
            removeBountyFromCache(bounty)
        }

        updated
    }

    suspend fun getCompletedBounties(completer: UUID): List<Bounty> = newSuspendedTransaction {
        Bounties
            .selectAll().where { Bounties.completer eq completer }
            .map { row ->
                Bounty(
                    id = row[Bounties.id],
                    creator = row[Bounties.creator],
                    target = row[Bounties.target],
                    createdAt = row[Bounties.createdAt],
                    duration = row[Bounties.duration],
                    reward = row[Bounties.reward],
                    completedAt = row[Bounties.completedAt],
                    completer = row[Bounties.completer]
                )
            }
    }

    suspend fun getTopNLeaderboard(n: Int, leaderboardType: LeaderboardType): List<BountyCompleteStats> =
        newSuspendedTransaction {
            val completer = Bounties.completer
            val completedCount = Bounties.id.count()
            val totalReward = Bounties.reward.sum()

            val orderByColumn = when (leaderboardType) {
                LeaderboardType.COMPLETED -> completedCount
                LeaderboardType.EARNINGS -> totalReward
            }

            Bounties
                .select(listOf(completer, completedCount, totalReward))
                .where {
                    (completer.isNotNull()) and (Bounties.completedAt.isNotNull())
                }
                .groupBy(completer)
                .orderBy(orderByColumn, SortOrder.DESC)
                .limit(n)
                .map { row ->
                    BountyCompleteStats(
                        completer = row[completer]!!,
                        completedCount = row[completedCount].toInt(),
                        totalReward = row[totalReward]?.toInt() ?: 0
                    )
                }
        }

    private fun hasRecentActiveBountyOnTarget(target: UUID, timeWindowMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        return bountiesByTarget[target]?.any { (it.createdAt + it.duration) >= (now - timeWindowMillis) && !it.isCompleted() } == true
    }

    private fun hasPlayerPlacedActiveBountyRecently(creator: UUID, timeWindowMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        return bountiesByCreator[creator]?.any { (it.createdAt + it.duration) >= (now - timeWindowMillis) && !it.isCompleted() } == true
    }

    suspend fun expireBounty(bountyId: Long): Boolean = newSuspendedTransaction {
        val bounty = activeBountiesCache[bountyId] ?: return@newSuspendedTransaction false
        if (bounty.isCompleted()) return@newSuspendedTransaction false

        Bounties.deleteWhere { id eq bountyId }
        removeBountyFromCache(bounty)
        true
    }

    fun getActiveBounties(): List<Bounty> = activeBountiesCache.values.filter { !it.isCompleted() }.toList()

    fun getActiveBountiesByCreator(creator: UUID): List<Bounty> =
        bountiesByCreator[creator]?.toList() ?: emptyList()

    fun getActiveBountyByTarget(target: UUID): Bounty? =
        bountiesByTarget[target]?.firstOrNull()

    fun isBountyExpired(bounty: Bounty): Boolean {
        val now = System.currentTimeMillis()
        return (bounty.createdAt + bounty.duration) <= now
    }
}
