/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.scheduler

import io.github.vedicmitra.core.common.result.AppResult
import io.github.vedicmitra.core.notifications.AppNotification
import kotlin.time.Instant

/**
 * Port for scheduling a notification to be posted at a future instant (the backbone of the alarm
 * feature). Implementations back this with the platform alarm service; when the app is not permitted
 * to schedule *exact* alarms they fall back to best-effort delivery, so callers should consult
 * [canScheduleExactAlarms] when precise timing matters and prompt the user to grant it.
 */
interface TaskScheduler {
    /**
     * Requests that [request] fires at [ScheduledTask.triggerAt], posting its
     * [ScheduledTask.notification]. Rescheduling with the same [ScheduledTask.id] replaces the
     * previous request.
     */
    suspend fun schedule(request: ScheduledTask): AppResult<Unit>

    /** Cancels a previously scheduled task by its [id]. No-op if nothing is scheduled. */
    suspend fun cancel(id: String): AppResult<Unit>

    /**
     * Whether the app can currently schedule *exact* alarms. Always true below API 31; on API 31+ it
     * reflects the user-grantable `SCHEDULE_EXACT_ALARM` permission. When false, [schedule] still
     * succeeds but delivery is inexact (the system may batch it to save power).
     */
    fun canScheduleExactAlarms(): Boolean
}

/**
 * A request to post a notification at a specific time.
 *
 * @property id stable, caller-owned identifier used to update or cancel the task.
 * @property triggerAt the instant the task should fire.
 * @property notification the notification to post when the task fires.
 * @property exact whether the platform must honour the exact time (vs. allowing batching). Honoured
 *   only when [canScheduleExactAlarms] is true.
 */
data class ScheduledTask(
    val id: String,
    val triggerAt: Instant,
    val notification: AppNotification,
    val exact: Boolean = true,
)
