package me.wolfity.bountymaster.commands

import me.wolfity.bountymaster.gui.BountyBoardGUI
import me.wolfity.bountymaster.plugin
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command

class BountyBoardCommand {

    @Command("bounties", "bountyboard", "bb")
    fun onBountyBoard(
        sender: Player
    ) {
        val activeBounties = plugin.bountyManager.getActiveBounties()
        BountyBoardGUI(plugin, sender, activeBounties)
    }

}