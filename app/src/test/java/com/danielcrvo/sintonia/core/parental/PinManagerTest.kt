package com.danielcrvo.sintonia.core.parental

import com.danielcrvo.sintonia.core.common.SecretStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinManagerTest {
    private class MemorySecrets : SecretStore {
        val map = HashMap<String, String>()
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String) {
            map[key] = value
        }
        override fun remove(key: String) {
            map.remove(key)
        }
    }

    private class FakeClock : PinClock {
        var wall = 1_000_000L
        var elapsed = 5_000L
        override fun wallMillis() = wall
        override fun elapsedMillis() = elapsed
    }

    private class MemoryAttempts : PinAttemptStore {
        var state = PinAttemptStore.Attempts(0)
        override suspend fun load() = state
        override suspend fun save(attempts: PinAttemptStore.Attempts) {
            state = attempts
        }
    }

    private val secrets = MemorySecrets()
    private val attempts = MemoryAttempts()
    private val clock = FakeClock()
    private val pin = PinManager(secrets, attempts, clock)

    @Test
    fun `does not store the PIN as plain text`() {
        pin.create("1234")
        assertTrue(pin.hasPin())
        assertFalse(secrets.map.values.any { "1234" in it })
    }

    @Test
    fun `accepts the right PIN and rejects the wrong one`() = runTest {
        pin.create("1234")
        assertEquals(PinCheck.Correct, pin.verify("1234"))
        assertEquals(PinCheck.Wrong, pin.verify("4321"))
    }

    @Test
    fun `PIN must have 4 digits`() {
        assertFailsWith<IllegalArgumentException> { pin.create("12") }
        assertFailsWith<IllegalArgumentException> { pin.create("12ab") }
    }

    @Test
    fun `five wrong attempts in a row lock for 30 seconds`() = runTest {
        pin.create("1234")
        repeat(4) { assertEquals(PinCheck.Wrong, pin.verify("0000")) }
        val locked = pin.verify("0000")
        assertTrue(locked is PinCheck.Locked)
        assertEquals(30_000L, (locked as PinCheck.Locked).remainingMillis)
        assertTrue(pin.verify("1234") is PinCheck.Locked)
    }

    @Test
    fun `after the lock it accepts again and the wait doubles on the next error`() = runTest {
        pin.create("1234")
        repeat(5) { pin.verify("0000") }
        clock.elapsed += 30_001
        clock.wall += 30_001
        val sixth = pin.verify("0000")
        assertEquals(60_000L, (sixth as PinCheck.Locked).remainingMillis)
    }

    @Test
    fun `a correct PIN resets the attempts`() = runTest {
        pin.create("1234")
        repeat(3) { pin.verify("0000") }
        assertEquals(PinCheck.Correct, pin.verify("1234"))
        assertEquals(0, attempts.state.failures)
        repeat(4) { assertEquals(PinCheck.Wrong, pin.verify("0000")) }
    }

    @Test
    fun `clear erases the PIN and the attempts`() = runTest {
        pin.create("1234")
        repeat(2) { pin.verify("0000") }
        pin.clear()
        assertFalse(pin.hasPin())
        assertEquals(0, attempts.state.failures)
    }

    @Test
    fun `changing the device time does not unlock`() = runTest {
        pin.create("1234")
        repeat(5) { pin.verify("0000") }
        clock.wall += 86_400_000
        clock.elapsed += 1_000
        assertTrue(pin.verify("1234") is PinCheck.Locked)
    }

    @Test
    fun `moving the time forward after a reboot does not unlock either`() = runTest {
        pin.create("1234")
        repeat(5) { pin.verify("0000") }
        // Rebooting and moving the date forward a day does not count as time served.
        clock.elapsed = 100
        clock.wall += 86_400_000
        val check = pin.verify("1234")
        assertEquals(30_000L, (check as PinCheck.Locked).remainingMillis)
        clock.elapsed += 30_001
        assertEquals(PinCheck.Correct, pin.verify("1234"))
    }

    @Test
    fun `moving the time back after a reboot does not unlock either`() = runTest {
        pin.create("1234")
        repeat(5) { pin.verify("0000") }
        // Rebooted (the monotonic clock restarts) and wall time moved before the lock.
        clock.elapsed = 100
        clock.wall -= 3_600_000
        val check = pin.verify("1234")
        assertTrue(check is PinCheck.Locked)
        assertEquals(30_000L, (check as PinCheck.Locked).remainingMillis)
    }
}
