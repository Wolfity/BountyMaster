package me.wolfity.bountymaster.manager

import java.util.UUID

data class Bounty(
    val id: Long,
    val creator: UUID,
    val target: UUID,
    val createdAt: Long,
    val duration: Long,
    val reward: Int,
    val completedAt: Long? = null,
    val completer: UUID? = null
) {
    fun isCompleted() = completedAt != null && completer != null
}