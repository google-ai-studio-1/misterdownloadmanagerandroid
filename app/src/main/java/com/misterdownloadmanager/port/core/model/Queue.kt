package com.misterdownloadmanager.port.core.model

import kotlinx.serialization.Serializable

@Serializable
data class QueueSchedule(
    val startOnStartup: Boolean = false,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val stopHour: Int? = null,
    val stopMinute: Int? = null,
    val days: List<Boolean> = List(7) { true },
    val retries: Int? = null,
    val onceDate: String? = null,
    val hangUp: Boolean = false,
    val exitWhenDone: Boolean = false,
    val turnOff: TurnOffAction? = null,
    val forceClose: Boolean = false,
    val wake: Boolean = false
)

enum class TurnOffAction {
    SHUT_DOWN,
    SLEEP,
    HIBERNATE
}

enum class QueueMemberState {
    WAITING,
    HELD,
    RUNNING,
    COMPLETE,
    FAILED
}

@Serializable
data class DownloadQueue(
    val name: String,
    val filesAtOnce: Int = 4,
    val itemIds: List<Long> = emptyList(),
    val sync: Boolean = false,
    val schedule: QueueSchedule = QueueSchedule()
) {
    companion object {
        const val RETRY_DELAY_MILLIS: Long = 30_000L
        const val DEFAULT_FILES_AT_ONCE: Int = 4
        const val MAX_FILES_AT_ONCE: Int = 99
    }

    fun filesAtOnceClamped(): Int = filesAtOnce.coerceIn(1, MAX_FILES_AT_ONCE)

    fun nextQueueName(existingNames: List<String>): String {
        var index = 1
        while (true) {
            val candidate = "Queue # $index"
            if (existingNames.none { it.equals(candidate, ignoreCase = true) }) return candidate
            index += 1
        }
    }
}

object QueueLogic {
    fun toStart(
        queue: DownloadQueue,
        members: Map<Long, QueueMemberState>,
        failedAtMillis: Map<Long, Long>,
        failedTries: Map<Long, Int>,
        nowMillis: Long
    ): List<Long> {
        val running = members.values.count { it == QueueMemberState.RUNNING }
        val freeSlots = queue.filesAtOnceClamped() - running
        if (freeSlots <= 0) return emptyList()
        val ready = mutableListOf<Long>()
        for (id in queue.itemIds) {
            if (ready.size >= freeSlots) break
            when (members[id]) {
                QueueMemberState.WAITING -> ready.add(id)
                QueueMemberState.FAILED -> {
                    val maxRetries = queue.schedule.retries
                    val tries = failedTries[id] ?: 0
                    val lastFailed = failedAtMillis[id] ?: 0L
                    if ((maxRetries == null || tries < maxRetries) && nowMillis - lastFailed >= DownloadQueue.RETRY_DELAY_MILLIS) {
                        ready.add(id)
                    }
                }
                else -> Unit
            }
        }
        return ready
    }

    fun isFinished(queue: DownloadQueue, members: Map<Long, QueueMemberState>, failedTries: Map<Long, Int>): Boolean {
        val maxRetries = queue.schedule.retries
        return queue.itemIds.all { id ->
            when (members[id]) {
                QueueMemberState.COMPLETE -> true
                QueueMemberState.FAILED -> maxRetries != null && (failedTries[id] ?: 0) >= maxRetries
                else -> false
            }
        }
    }
}
