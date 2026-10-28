package com.danielcrvo.sintonia.core.parental

import com.danielcrvo.sintonia.core.common.SecretStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PinPromptControllerTest {
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

    private val pins = PinManager(MemorySecrets(), MemoryAttempts(), FakeClock())

    private fun controller(scope: TestScope) = PinPromptController(pins, TestScope(UnconfinedTestDispatcher(scope.testScheduler)))

    private fun PinPromptController.type(code: String) = code.forEach { onDigit(it.digitToInt()) }

    @Test
    fun `create asks twice and saves when they match`() = runTest {
        val controller = controller(this)
        var created = false
        controller.create { created = true }

        controller.type("1234")
        assertTrue(controller.state.value!!.confirming)
        controller.type("1234")

        assertTrue(created)
        assertNull(controller.state.value)
        assertTrue(pins.hasPin())
    }

    @Test
    fun `create with different PINs shows an error and restarts`() = runTest {
        val controller = controller(this)
        controller.create { error("should not create") }

        controller.type("1234")
        controller.type("1235")

        val state = controller.state.value!!
        assertEquals(PinError.Mismatch, state.error)
        assertEquals(false, state.confirming)
        assertEquals("", state.digits)
        assertEquals(false, pins.hasPin())
    }

    @Test
    fun `a correct check runs the action and closes`() = runTest {
        pins.create("2580")
        val controller = controller(this)
        var done = false
        controller.verify(PinPurpose.Unlock) { done = true }

        controller.type("2580")

        assertTrue(done)
        assertNull(controller.state.value)
    }

    @Test
    fun `wrong PIN clears the boxes and counts a new shake on each error`() = runTest {
        pins.create("2580")
        val controller = controller(this)
        controller.verify(PinPurpose.Unlock) { error("should not open") }

        controller.type("0000")
        val first = controller.state.value!!
        controller.type("1111")
        val second = controller.state.value!!

        assertEquals(PinError.Wrong, first.error)
        assertEquals("", first.digits)
        assertEquals(first.errorTick + 1, second.errorTick)
    }

    @Test
    fun `after several errors it reports the lock with the seconds`() = runTest {
        pins.create("2580")
        val controller = controller(this)
        controller.verify(PinPurpose.Unlock) {}
        repeat(5) { controller.type("0000") }
        val state = controller.state.value!!
        assertEquals(PinError.Locked, state.error)
        assertEquals(30, state.lockedSeconds)
    }

    @Test
    fun `backspace removes the last digit and clears the error`() = runTest {
        val controller = controller(this)
        controller.create {}
        controller.onDigit(1)
        controller.onDigit(2)
        controller.onBackspace()
        assertEquals("1", controller.state.value!!.digits)
        assertNotNull(controller.state.value)
    }
}
