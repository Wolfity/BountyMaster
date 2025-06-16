package me.wolfity.bountymaster.manager

import java.util.UUID

data class BountyCompleteStats(
    val completer: UUID,
    val completedCount: Int,
    val totalReward: Int
)