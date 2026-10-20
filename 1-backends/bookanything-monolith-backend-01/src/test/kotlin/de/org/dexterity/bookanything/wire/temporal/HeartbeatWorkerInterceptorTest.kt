package de.org.dexterity.bookanything.wire.temporal

import io.mockk.every
import io.mockk.mockk
import io.temporal.activity.ActivityExecutionContext
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor.ActivityInput
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor.ActivityOutput
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartbeatWorkerInterceptorTest {

    private val output = mockk<ActivityOutput>()
    private val input = mockk<ActivityInput>()
    private val context = mockk<ActivityExecutionContext>()
    private val beats = AtomicInteger()

    private fun intercepted(body: () -> ActivityOutput): ActivityInboundCallsInterceptor {
        val next = mockk<ActivityInboundCallsInterceptor>(relaxed = true)
        every { next.execute(any()) } answers { body() }
        return HeartbeatWorkerInterceptor(periodSeconds = 1).interceptActivity(next).also { it.init(context) }
    }

    @Test
    fun `beats while the activity runs and stops once it returns`() {
        every { context.heartbeat(null) } answers { beats.incrementAndGet(); Unit }
        val interceptor = intercepted { Thread.sleep(2500); output }

        assertEquals(output, interceptor.execute(input))

        val atReturn = beats.get()
        assertTrue(atReturn >= 2, "expected at least 2 heartbeats during a 2.5 s activity, got $atReturn")
        Thread.sleep(1500)
        assertEquals(atReturn, beats.get(), "no heartbeat may follow the end of the activity")
    }

    @Test
    fun `stops beating when the activity throws`() {
        every { context.heartbeat(null) } answers { beats.incrementAndGet(); Unit }
        val interceptor = intercepted { Thread.sleep(1200); throw IllegalStateException("boom") }

        runCatching { interceptor.execute(input) }
        val atFailure = beats.get()
        Thread.sleep(1500)

        assertEquals(atFailure, beats.get())
    }

    @Test
    fun `a failing heartbeat does not fail the activity`() {
        every { context.heartbeat(null) } throws RuntimeException("server unreachable")
        val interceptor = intercepted { Thread.sleep(1200); output }

        assertEquals(output, interceptor.execute(input))
    }
}
