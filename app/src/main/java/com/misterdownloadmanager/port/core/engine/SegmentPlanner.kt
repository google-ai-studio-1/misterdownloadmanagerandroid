package com.misterdownloadmanager.port.core.engine

const val SEGMENT_ALIGNMENT_BYTES: Long = 4096L

data class Segment(
    val start: Long,
    var end: Long,
    var from: Long,
    var worker: Int? = null,
    var received: Boolean = false,
    var givenAtMillis: Long? = null
) {
    fun unclaimed(): Long = end - from

    fun isFinished(): Boolean = from >= end
}

data class SavedSegment(
    val start: Long,
    val end: Long,
    val from: Long,
    val preFetchedBytes: Long = 0L
)

sealed class Claim {
    data class Write(val bytes: Long) : Claim()
    object Lost : Claim()
    object Done : Claim()
}

class InvalidResumeState(message: String) : IllegalArgumentException(message)

class SegmentPlanner(
    private val size: Long,
    minSplit: Long
) {
    private val minSplit: Long = minSplit.coerceAtLeast(1L)
    private val segments = mutableListOf<Segment>()
    private var preemptAfterMillis: Long = 0L

    fun setPreemptAfter(millis: Long) {
        preemptAfterMillis = millis.coerceAtLeast(0L)
    }

    init {
        if (size > 0L) {
            segments.add(Segment(start = 0L, end = size, from = 0L))
        }
    }

    companion object {
        fun resume(size: Long, minSplit: Long, saved: List<SavedSegment>): SegmentPlanner {
            val planner = SegmentPlanner(size, minSplit)
            planner.segments.clear()
            var expected = 0L
            for (item in saved) {
                if (item.start != expected) {
                    throw InvalidResumeState("Saved segments do not tile [0, size) exactly.")
                }
                if (item.end <= item.start || item.from < item.start || item.from > item.end) {
                    throw InvalidResumeState("Saved segment is out of range.")
                }
                planner.segments.add(
                    Segment(start = item.start, end = item.end, from = item.from)
                )
                expected = item.end
            }
            if (expected != size) {
                throw InvalidResumeState("Saved segments do not tile [0, size) exactly.")
            }
            return planner
        }
    }

    fun save(): List<SavedSegment> {
        return segments.map {
            SavedSegment(start = it.start, end = it.end, from = it.from, preFetchedBytes = 0L)
        }
    }

    fun segments(): List<Segment> = segments.toList()

    fun activeWorkers(): Int {
        return segments.count { it.worker != null && it.received && !it.isFinished() }
    }

    fun claim(worker: Int, after: Long?): Claim {
        val open = segments.filter { it.worker == null && it.unclaimed() > 0L }
        val continuing = after?.let { end ->
            open.firstOrNull { it.start == end }
        }
        val chosen = continuing ?: open.firstOrNull()
        if (chosen != null) {
            chosen.worker = worker
            chosen.givenAtMillis = System.currentTimeMillis()
            return Claim.Write(chosen.unclaimed())
        }
        if (preemptAfterMillis > 0L && after != null) {
            val now = System.currentTimeMillis()
            val victim = segments.firstOrNull { segment ->
                segment.start == after && segment.worker != null && !segment.received &&
                    segment.unclaimed() > 0L &&
                    (segment.givenAtMillis?.let { now - it >= preemptAfterMillis } == true)
            }
            if (victim != null) {
                victim.worker = worker
                victim.givenAtMillis = now
                return Claim.Write(victim.unclaimed())
            }
        }
        val largestIndex = segments.indices
            .filter { segments[it].unclaimed() > 0L }
            .maxByOrNull { segments[it].unclaimed() }
            ?: return Claim.Done
        val largest = segments[largestIndex]
        val half = largest.unclaimed() / 2L
        if (half < minSplit) return Claim.Done
        val aligned = ((largest.from + half) / SEGMENT_ALIGNMENT_BYTES) * SEGMENT_ALIGNMENT_BYTES
        val splitAt = if (aligned >= largest.from + minSplit) aligned else largest.from + half
        val upper = Segment(start = splitAt, end = largest.end, from = splitAt, worker = worker, givenAtMillis = System.currentTimeMillis())
        largest.end = splitAt
        segments.add(largestIndex + 1, upper)
        return Claim.Write(upper.unclaimed())
    }

    fun markReceived(worker: Int, position: Long) {
        val segment = segments.firstOrNull { it.worker == worker } ?: return
        segment.received = true
        segment.from = position.coerceIn(segment.start, segment.end)
    }

    fun release(worker: Int) {
        val segment = segments.firstOrNull { it.worker == worker } ?: return
        segment.worker = null
        segment.received = false
        segment.givenAtMillis = null
    }

    fun remainingBytes(): Long = segments.sumOf { it.unclaimed() }

    fun isComplete(): Boolean = segments.all { it.isFinished() }
}
