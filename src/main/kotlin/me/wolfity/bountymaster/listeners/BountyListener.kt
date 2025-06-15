package me.wolfity.bountymaster.listeners

import me.wolfity.bountymaster.events.BountyCancelledEvent
import me.wolfity.bountymaster.events.BountyCompletedEvent
import me.wolfity.bountymaster.events.BountyExpireEvent
import me.wolfity.bountymaster.events.BountyPlacedEvent
import me.wolfity.bountymaster.plugin
import me.wolfity.developmentutil.ext.call
import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.player.PlayerData
import me.wolfity.developmentutil.util.TimeFormatConfig
import me.wolfity.developmentutil.util.TimeUnitFormat
import me.wolfity.developmentutil.util.formatTime
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import me.wolfity.developmentutil.util.style
import org.bukkit.Bukkit
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import kotlin.math.roundToInt

class BountyListener : Listener {

    @EventHandler
    fun onBountyExpire(event: BountyExpireEvent) {
        val bounty = event.bounty
        launchAsync {
            val creator = plugin.playerRegistry.getDataByUUID(bounty.creator)!!
            val target = plugin.playerRegistry.getDataByUUID(bounty.target)!!
            Bukkit.getOnlinePlayers().forEach {
                it.sendStyled(
                    plugin.config.getString("bounty-expire-message")!!
                        .replace("{creator}", creator.name)
                        .replace("{target}", target.name)
                )
            }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val killed = event.entity as? Player ?: return
        val killer = when (val damager = event.damager) {
            is Player -> {
                damager
            }

            is Projectile -> {
                (damager.shooter as? Player)
            }

            else -> null
        } ?: return


        val bountyByKilled = plugin.bountyManager.getBountyByTarget(killed.uuid) ?: return
        if (event.finalDamage >= killed.health) {
            launchAsync {
                plugin.bountyManager.completeBounty(bountyByKilled.id, killer.uuid)
            }
            BountyCompletedEvent(killer, killed, bountyByKilled).call(plugin)
        }
    }

    @EventHandler
    fun onBountyComplete(event: BountyCompletedEvent) {
        Bukkit.broadcast(
            style(
                plugin.config.getString("bounty-completed-announcement")!!
                    .replace("{player}", event.completer.name)
                    .replace("{target}", event.target.name)
                    .replace("{reward}", event.bounty.reward.toString())
            )
        )
    }

    @EventHandler
    fun onBountyCancel(event: BountyCancelledEvent) {
        val announcement = plugin.config.getString("cancel-bounty-announcement")!!
            .replace("{target}", event.targetPlayer)
            .replace("{player}", event.canceller.name)

        Bukkit.getOnlinePlayers().forEach {
            it.sendStyled(announcement)
        }
    }

    @EventHandler
    fun onBountyPlace(event: BountyPlacedEvent) {
        val announcement =
            announceBountyCreation(event.creator, event.target, event.bounty.duration, event.bounty.reward)
        Bukkit.getOnlinePlayers().forEach {
            it.sendStyled(announcement)
        }

    }

    private fun announceBountyCreation(
        sender: Player,
        targetUser: String,
        bountyDurationMillis: Long,
        amount: Int
    ): String {
        return plugin.config.getString("bounty-announcement")!!
            .replace("{creator}", sender.name)
            .replace("{target}", targetUser)
            .replace("{rewardAmount}", amount.toString())
            .replace(
                "{expiry}",
                formatTime(
                    bountyDurationMillis,
                    TimeFormatConfig(listOf(TimeUnitFormat.HOURS, TimeUnitFormat.MINUTES))
                )
            )
    }

}