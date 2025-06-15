package me.wolfity.bountymaster.events

import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.event.CustomEvent
import org.bukkit.entity.Player

class BountyPlacedEvent(val bounty: Bounty, val creator: Player, val target: String) : CustomEvent() {
}