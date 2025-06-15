package me.wolfity.bountymaster.tasks

import me.wolfity.bountymaster.events.BountyExpireEvent
import me.wolfity.bountymaster.plugin
import me.wolfity.developmentutil.util.launchAsync
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class BountyHealthCheckTask : BukkitRunnable() {

    private val shouldRefund: Boolean by lazy {
        plugin.config.getBoolean("return-money-on-bounty-expire")
    }

    override fun run() {
        val now = System.currentTimeMillis()

        launchAsync {
            plugin.bountyManager.getActiveBounties().forEach { bounty ->
                val expireTime = bounty.createdAt + bounty.duration

                if (now >= expireTime && !bounty.isCompleted()) {
                    val creatorData = plugin.playerRegistry.getDataByUUID(bounty.creator)
                    plugin.bountyManager.expireBounty(bountyId = bounty.id)

                    // Run refund and event call on main thread
                    object : BukkitRunnable() {
                        override fun run() {
                            creatorData?.let {
                                if (shouldRefund) {
                                    plugin.economy.depositPlayer(it.name, bounty.reward.toDouble())
                                }
                            }
                            Bukkit.getPluginManager().callEvent(BountyExpireEvent(bounty))
                        }
                    }.runTask(plugin)
                }
            }
        }
    }
}
