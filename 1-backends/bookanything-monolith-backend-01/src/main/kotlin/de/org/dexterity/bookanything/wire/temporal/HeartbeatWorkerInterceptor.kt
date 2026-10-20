package de.org.dexterity.bookanything.wire.temporal

import io.temporal.activity.ActivityExecutionContext
import io.temporal.common.interceptors.ActivityInboundCallsInterceptor
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase
import io.temporal.common.interceptors.WorkerInterceptorBase
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Heartbeats every running activity on a timer.
 *
 * Temporal only notices an activity task that was handed to a worker and never executed (seen as
 * "Activity task already started" in the server log) when StartToClose expires: 5 min for the maps step,
 * 20 min for the cleanup delete. With a short HeartbeatTimeout on the activity options and this
 * interceptor beating while the activity really runs, a lost task is retried after that short timeout
 * instead, and long activities are not cut off.
 */
class HeartbeatWorkerInterceptor(private val periodSeconds: Long = 5) : WorkerInterceptorBase() {

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "temporal-activity-heartbeat").apply { isDaemon = true }
    }

    override fun interceptActivity(next: ActivityInboundCallsInterceptor): ActivityInboundCallsInterceptor =
        object : ActivityInboundCallsInterceptorBase(next) {
            private lateinit var context: ActivityExecutionContext

            override fun init(context: ActivityExecutionContext) {
                this.context = context
                super.init(context)
            }

            override fun execute(input: ActivityInboundCallsInterceptor.ActivityInput): ActivityInboundCallsInterceptor.ActivityOutput {
                val beat = scheduler.scheduleWithFixedDelay(
                    { runCatching { context.heartbeat(null) } },
                    0, periodSeconds, TimeUnit.SECONDS
                )
                try {
                    return super.execute(input)
                } finally {
                    beat.cancel(false)
                }
            }
        }
}
