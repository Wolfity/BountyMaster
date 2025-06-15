package me.wolfity.bountymaster.commands

import me.wolfity.bountymaster.gui.CompletedBountiesGUI
import me.wolfity.bountymaster.plugin
import me.wolfity.developmentutil.ext.uuid
import me.wolfity.developmentutil.util.launchAsync
import me.wolfity.developmentutil.util.sendStyled
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Optional

class BountyHistoryCommand {

    @Command("bountyhistory", "bhistory", "bountieshistory", "completedbounties")
    fun onBountyHistory(
        sender: Player,
        @Named("player") @Optional target: UserCommandParameter?
    ) {
        launchAsync {
            val targetUser = if (target == null) sender.uuid else plugin.playerRegistry.getDataByName(target.name)?.uuid
            if (targetUser == null) {
                sender.sendStyled("<red>This player does not exist")
                return@launchAsync
            }

            val bounties = plugin.bountyManager.getCompletedBounties(targetUser)
            CompletedBountiesGUI(plugin, sender, bounties)

        }
    }
}