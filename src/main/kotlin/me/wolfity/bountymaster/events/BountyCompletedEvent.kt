package me.wolfity.bountymaster.events

import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.event.CustomEvent
import org.bukkit.entity.Player

class BountyCompletedEvent(val completer: Player, val target: Player, val bounty: Bounty) : CustomEvent() {
}