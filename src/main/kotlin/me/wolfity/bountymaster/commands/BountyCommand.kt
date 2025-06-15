package me.wolfity.bountymaster.commands

import me.wolfity.bountymaster.events.BountyCancelledEvent
import me.wolfity.bountymaster.events.BountyPlacedEvent
import me.wolfity.bountymaster.plugin
import me.wolfity.developmentutil.ext.call
import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.minutesToMillis
import me.wolfity.developmentutil.util.secondsToMillis
import me.wolfity.developmentutil.util.sendStyled
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.Range

class BountyCommand {

    @Command("placebounty")
    fun onPlaceBounty(
        sender: Player,
        @Named("amount") @Range(min = 0.0) amount: Int,
        @Named("player") @Optional target: UserCommandParameter,
    ) {
        if (target.name.equals(sender.name, ignoreCase = true)) {
            sender.sendStyled("<red>You can't place a bounty on yourself!")
            return
        }

        val currentMoney = plugin.economy.getBalance(sender)
        if (currentMoney < amount) {
            sender.sendStyled("<red>You don't have enough money to place this bounty!")
            return
        }

        launchAsync {
            val targetUser = plugin.playerRegistry.getDataByName(target.name)
            if (targetUser == null) {
                sender.sendStyled("<red>This user does not exist!")
                return@launchAsync
            }

            val cooldownMinutes = plugin.config.getLong("bounty-creation-cooldown-minutes")
            val cooldownMillis = secondsToMillis(cooldownMinutes)

            if (plugin.bountyManager.hasPlayerPlacedBountyRecently(sender.uuid, cooldownMillis)) {
                val cooldownMessage = plugin.config.getString("placed-bounty-recently")
                    ?.replace("{time}", cooldownMinutes.toString())
                sender.sendStyled(cooldownMessage ?: "<red>You must wait before placing another bounty.")
                return@launchAsync
            }

            val targetCooldownSeconds = plugin.config.getLong("bounty-target-cooldown-seconds")
            val targetCooldownMillis = secondsToMillis(targetCooldownSeconds)

            if (plugin.bountyManager.hasHadBountyInTimeWindow(targetUser.uuid, targetCooldownMillis)) {
                sender.sendStyled(plugin.config.getString("received-bounty-recently")!!)
                return@launchAsync
            }

            val bountyDurationMinutes = plugin.config.getLong("bounty-duration-minutes")
            val bountyDurationMillis = minutesToMillis(bountyDurationMinutes)

            val bounty = plugin.bountyManager.startBounty(
                creator = sender.uuid,
                target = targetUser.uuid,
                durationMillis = bountyDurationMillis,
                reward = amount
            )

            plugin.economy.withdrawPlayer(sender, amount.toDouble())

            BountyPlacedEvent(bounty, sender, targetUser.name).call(plugin)
        }
    }


    @Command("cancelbounty")
    fun onCancelBounty(sender: Player, @Named("target") target: UserCommandParameter) {
        launchAsync {
            val targetPlayer = plugin.playerRegistry.getDataByName(target.name)
            if (targetPlayer == null) {
                sender.sendStyled(
                    plugin.config.getString("cancel-bounty-player-not-found")!!
                        .replace("{target}", target.name)
                )
                return@launchAsync
            }

            val activeBounties = plugin.bountyManager.getBountiesByCreator(sender.uuid)
                .filter { it.target == targetPlayer.uuid }

            if (activeBounties.isEmpty()) {
                sender.sendStyled(
                    plugin.config.getString("cancel-bounty-no-active")!!
                        .replace("{target}", targetPlayer.name)
                )
                return@launchAsync
            }

            val bountyToCancel = activeBounties.maxByOrNull { it.createdAt }!!

            plugin.bountyManager.cancelBounty(bountyToCancel.id)
            plugin.economy.depositPlayer(sender, bountyToCancel.reward.toDouble())

            sender.sendStyled(
                plugin.config.getString("cancel-bounty-success")!!
                    .replace("{target}", targetPlayer.name)
            )

            BountyCancelledEvent(bountyToCancel, sender, targetPlayer.name).call(plugin)

        }
    }
}