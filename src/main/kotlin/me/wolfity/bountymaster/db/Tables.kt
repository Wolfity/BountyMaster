package me.wolfity.bountymaster.db

import org.jetbrains.exposed.sql.Table

object Bounties : Table("bounties") {
    val id = long("id").autoIncrement()
    val creator = uuid("creator")
    val target = uuid("target")
    val createdAt = long("createdAt")
    val duration = long("duration")
    val reward = integer("reward")
    val completedAt = long("completed_at").nullable()
    val completer = uuid("completer").nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(id)

    init {
        index("bounties_completer", false, completer)
        index("bounties_creator", false, creator)
        index("bounties_target", false, target)
        index("bounties_createdAt", false, createdAt)
        index("bounties_target_createdAt", false, target, createdAt)
        index("bounties_creator_createdAt", false, creator, createdAt)
    }
}