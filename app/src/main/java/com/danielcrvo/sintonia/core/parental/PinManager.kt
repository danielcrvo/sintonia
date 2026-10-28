package com.danielcrvo.sintonia.core.parental

import com.danielcrvo.sintonia.core.common.SecretStore
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinCheck {
    data object Correct : PinCheck
    data object Wrong : PinCheck

    data class Locked(val remainingMillis: Long) : PinCheck
}

interface PinAttemptStore {
    suspend fun load(): Attempts
    suspend fun save(attempts: Attempts)

    data class Attempts(
        val failures: Int,
        val lockedAtWall: Long = 0,
        val lockedAtElapsed: Long = 0,
        val durationMillis: Long = 0,
    )
}

/** Wall clock changes if the user edits the time; the monotonic one does not. */
interface PinClock {
    fun wallMillis(): Long
    fun elapsedMillis(): Long
}

@Singleton
class SystemPinClock
    @Inject
    constructor() : PinClock {
        override fun wallMillis() = System.currentTimeMillis()
        override fun elapsedMillis() = android.os.SystemClock.elapsedRealtime()
    }

@Singleton
class DataStorePinAttempts
    @Inject
    constructor(private val preferences: AppPreferences) : PinAttemptStore {
        override suspend fun load(): PinAttemptStore.Attempts {
            val (failures, wall, elapsed, duration) = preferences.pinLock.first()
            return PinAttemptStore.Attempts(failures.toInt(), wall, elapsed, duration)
        }

        override suspend fun save(attempts: PinAttemptStore.Attempts) =
            preferences.setPinLock(attempts.failures, attempts.lockedAtWall, attempts.lockedAtElapsed, attempts.durationMillis)
    }

@Singleton
class PinManager
    @Inject
    constructor(
        private val secrets: SecretStore,
        private val attempts: PinAttemptStore,
        private val clock: PinClock,
    ) {
        private val mutex = Mutex()

        fun hasPin(): Boolean = secrets.get(PinKey) != null

        fun create(pin: String) {
            require(isValid(pin)) { "PIN must have 4 digits" }
            val salt = ByteArray(SaltBytes).also(SecureRandom()::nextBytes)
            secrets.put(PinKey, encode(salt) + ":" + encode(hash(pin, salt)))
        }

        /** Serialized so concurrent checks do not lose failures from the count. */
        suspend fun verify(pin: String): PinCheck = mutex.withLock {
            val state = attempts.load()
            val remaining = remainingLockMillis(state)
            if (remaining > 0) return@withLock PinCheck.Locked(remaining)

            val stored = secrets.get(PinKey) ?: return@withLock PinCheck.Wrong
            val (salt, expected) = stored.split(':').let { decode(it[0]) to decode(it[1]) }
            // Constant-time comparison so it does not leak how many digits match.
            val correct = isValid(pin) && MessageDigest.isEqual(hash(pin, salt), expected)
            if (correct) {
                attempts.save(PinAttemptStore.Attempts(0))
                return@withLock PinCheck.Correct
            }
            val failures = state.failures + 1
            if (failures < MaxFailures) {
                attempts.save(PinAttemptStore.Attempts(failures))
                return@withLock PinCheck.Wrong
            }
            val duration = lockMillis(failures)
            attempts.save(PinAttemptStore.Attempts(failures, clock.wallMillis(), clock.elapsedMillis(), duration))
            PinCheck.Locked(duration)
        }

        /** After a reboot the monotonic clock restarts and wall time is untrusted: the lock restarts in full from now. */
        private suspend fun remainingLockMillis(state: PinAttemptStore.Attempts): Long {
            if (state.durationMillis <= 0) return 0
            val sinceElapsed = clock.elapsedMillis() - state.lockedAtElapsed
            if (sinceElapsed < 0) {
                attempts.save(state.copy(lockedAtWall = clock.wallMillis(), lockedAtElapsed = clock.elapsedMillis()))
                return state.durationMillis
            }
            return (state.durationMillis - sinceElapsed).coerceAtLeast(0)
        }

        suspend fun clear() {
            secrets.remove(PinKey)
            attempts.save(PinAttemptStore.Attempts(0, 0))
        }

        private fun lockMillis(failures: Int): Long =
            BaseLockMillis shl (failures - MaxFailures).coerceAtMost(MaxDoublings)

        private fun isValid(pin: String) = pin.length == PinLength && pin.all { it.isDigit() }

        private fun hash(pin: String, salt: ByteArray): ByteArray =
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(PBEKeySpec(pin.toCharArray(), salt, Iterations, KeyBits))
                .encoded

        private fun encode(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
        private fun decode(hex: String) = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

        companion object {
            const val PinLength = 4
            const val MaxFailures = 5
            const val BaseLockMillis = 30_000L
            private const val MaxDoublings = 6
            private const val PinKey = "parental.pin"
            private const val SaltBytes = 16
            private const val Iterations = 20_000
            private const val KeyBits = 256
        }
    }
