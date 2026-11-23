package com.misterdownloadmanager.port.core.engine

import kotlin.math.exp

class SpeedTracker {
    private var averageBytesPerSecond: Double = 0.0
    private var lastUpdateNanos: Long = System.nanoTime()
    private var lastBytesTimestampNanos: Long = System.nanoTime()
    private var shownSpeedBytesPerSecond: Long = 0L
    private var lastShownNanos: Long = System.nanoTime()

    fun addBytes(bytes: Long) {
        val now = System.nanoTime()
        val elapsedSeconds = (now - lastUpdateNanos).toDouble() / 1_000_000_000.0
        lastUpdateNanos = now
        if (bytes > 0L) lastBytesTimestampNanos = now
        if (elapsedSeconds <= 0.0) return
        val instant = bytes.toDouble() / elapsedSeconds
        val alpha = 1.0 - exp(-elapsedSeconds / SPEED_TAU_SECONDS)
        averageBytesPerSecond = averageBytesPerSecond + (instant - averageBytesPerSecond) * alpha
        if ((now - lastShownNanos).toDouble() / 1_000_000_000.0 >= SHOWN_EVERY_SECONDS) {
            lastShownNanos = now
            shownSpeedBytesPerSecond = currentSpeedBytesPerSecond()
        }
    }

    fun currentSpeedBytesPerSecond(): Long {
        val idleSeconds = (System.nanoTime() - lastBytesTimestampNanos).toDouble() / 1_000_000_000.0
        if (idleSeconds >= STILL_SECONDS) return 0L
        return averageBytesPerSecond.toLong()
    }

    fun shownSpeedBytesPerSecond(): Long = shownSpeedBytesPerSecond

    companion object {
        const val SPEED_TAU_SECONDS: Double = 3.0
        const val SHOWN_EVERY_SECONDS: Double = 0.5
        const val STILL_SECONDS: Double = 5.0
        const val SPLIT_TAU_SECONDS: Double = 2.0
    }
}
