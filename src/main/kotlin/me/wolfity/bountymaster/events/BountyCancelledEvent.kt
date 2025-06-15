package me.wolfity.bountymaster.events

import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.event.CustomEvent
import org.bukkit.entity.Player

class BountyCancelledEvent(val bounty: Bounty, val canceller: Player, val targetPlayer: String) : CustomEvent() {
}