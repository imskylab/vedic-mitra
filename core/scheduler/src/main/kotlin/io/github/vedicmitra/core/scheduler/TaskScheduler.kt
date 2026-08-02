/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.scheduler

import io.github.vedicmitra.core.common.result.AppResult
import kotlin.time.Instant

/**
 * Port for scheduling work to run at a future instant (the backbone of the alarm feature).
 *
 * **No scheduling is implemented in Phase 1.** The concrete implementation (AlarmManager for exact
 * alarms, WorkManager for deferrable work) is added in the scheduler implementation phase.
 */
interface TaskScheduler {
    /**
     * Requests that the task identified by [request] fires at [ScheduledTask.triggerAt].
     * Rescheduling with the same [ScheduledTask.id] replaces the previous request.
     */
    suspend fun schedule(request: ScheduledTask): AppResult<Unit>

    /** Cancels a previously scheduled task by its [id]. No-op if nothing is scheduled. */
    suspend fun cancel(id: String): AppResult<Unit>
}

/**
 * A request to run something at a specific time.
 *
 * @property id stable, caller-owned identifier used to update or cancel the task.
 * @property triggerAt the instant the task should fire.
 * @property exact whether the platform must honour the exact time (vs. allowing batching).
 */
data class ScheduledTask(
    val id: String,
    val triggerAt: Instant,
    val exact: Boolean = true,
)
