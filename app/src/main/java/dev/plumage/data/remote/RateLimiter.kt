package dev.plumage.data.remote

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarantees at least [MIN_INTERVAL_MS] between the *starts* of two API calls.
 *
 * This lives at the repository layer rather than in an OkHttp interceptor for two
 * reasons. First, an interceptor would have to Thread.sleep() and pin a dispatcher
 * thread; suspending here costs nothing. Second, Coil shares an OkHttp stack, and
 * a throttling interceptor on a shared client would rate-limit image downloads to
 * one per second, which would make the deck unusable. CDN fetches are not API
 * calls and must not be gated.
 *
 * The lock is released before the call runs, so a slow response never blocks the
 * next request past its interval.
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val mutex = Mutex()
    private var lastStartedAt = 0L

    suspend fun <T> gate(block: suspend () -> T): T {
        mutex.withLock {
            val waitMs = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastStartedAt)
            if (waitMs > 0) delay(waitMs)
            lastStartedAt = SystemClock.elapsedRealtime()
        }
        return block()
    }

    companion object {
        const val MIN_INTERVAL_MS = 1_000L
    }
}
