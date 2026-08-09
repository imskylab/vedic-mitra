/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.core.common.coroutines.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms persisted reminders after the platform clears alarms — on device reboot
 * ([Intent.ACTION_BOOT_COMPLETED]) and after the app is updated/reinstalled
 * ([Intent.ACTION_MY_PACKAGE_REPLACED]), which also cancels pending alarms. The suspending
 * rescheduling is hoisted onto a coroutine kept alive by [goAsync] until it completes.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var rescheduler: ReminderRescheduler

    @Inject
    lateinit var dispatchers: DispatcherProvider

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + dispatchers.default).launch {
            try {
                rescheduler.rescheduleEnabled(System.currentTimeMillis())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
