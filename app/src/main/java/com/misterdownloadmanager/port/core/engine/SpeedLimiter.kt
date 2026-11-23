package com.misterdownloadmanager.port.core.engine

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

const val LIMITER_BURST_SECONDS: Double = 1.0

class SpeedLimiter(initialRateBytesPerSecond: Long?) {
    private val rate = AtomicLong(initialRateBytesPerSecond ?: -1L)
    private val tokens = AtomicLong(0L)
    private val lastRefillNanos = AtomicLong(System.nanoTime())
    private val takenTotal = AtomicLong(0L)

    fun set(rateBytesPerSecond: Long?) {
        val previous = rate.getAndSet(rateBytesPerSecond ?: -1L)
        if (previous == (rateBytesPerSecond ?: -1L)) return
        if (rateBytesPerSecond == null) {
            tokens.set(0L)
        }
    }

    fun get(): Long? {
        val value = rate.get()
        return if (value < 0L) null else value
    }

    fun taken(): Long = takenTotal.get()

    fun refund(bytes: Long) {
        if (bytes <= 0L) return
        takenTotal.addAndGet(-bytes)
        tokens.addAndGet(bytes)
    }

    private fun refill() {
        val currentRate = rate.get()
        if (currentRate < 0L) return
        val now = System.nanoTime()
        val previous = lastRefillNanos.getAndSet(now)
        val elapsedSeconds = (now - previous).toDouble() / 1_000_000_000.0
        if (elapsedSeconds <= 0.0) return
        val burst = (currentRate.toDouble() * LIMITER_BURST_SECONDS).toLong()
        val added = (currentRate.toDouble() * elapsedSeconds).toLong()
        val current = tokens.get()
        tokens.set(min(burst, current + added))
    }

    suspend fun acquire(bytes: Long) {
        val currentRate = rate.get()
        if (currentRate < 0L || bytes <= 0L) {
            takenTotal.addAndGet(bytes)
            return
        }
        var remaining = bytes
        while (remaining > 0L) {
            refill()
            val available = tokens.get()
            if (available > 0L) {
                val take = min(available, remaining)
                tokens.addAndGet(-take)
                takenTotal.addAndGet(take)
                remaining -= take
            } else {
                val waitMillis = ((remaining * 1000L) / currentRate).coerceAtLeast(1L)
                delay(waitMillis)
            }
        }
    }
}
