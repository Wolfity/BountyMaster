package me.wolfity.bountymaster.events

import me.wolfity.bountymaster.manager.Bounty
import me.wolfity.developmentutil.event.CustomEvent

class BountyExpireEvent(val bounty: Bounty) : CustomEvent() {
}