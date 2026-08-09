/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Foreground service that rings an alarm-mode reminder. Started by the scheduler's receiver when an
 * alarm fires, it plays the alarm tone and vibrates (via [AlarmRinger]) while showing the ongoing
 * full-screen-intent notification built by [AlarmAlert].
 *
 * Ringing lives here — not in [AlarmActivity] — so the alarm sounds even when the system demotes the
 * full-screen intent to a plain notification (the default for apps that have not been granted the
 * full-screen-intent permission on Android 14+). The activity, when it launches, is only the
 * lock-screen UI; its Dismiss action and the notification's Dismiss action both stop this service. A
 * safety timeout stops the alarm after two minutes if the user never dismisses it.
 */
class AlarmService : Service() {
    private val autoStop = Handler(Looper.getMainLooper())
    private var activeId: Int = 0
    private var ringing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val id = intent?.getIntExtra(EXTRA_ID, 0) ?: 0
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent?.getStringExtra(EXTRA_BODY).orEmpty()
        activeId = id

        // Becoming a foreground service can be refused on some OEMs; guard it so we neither crash nor
        // skip the ring. The alarm notification is already posted by the receiver either way.
        runCatching { startForegroundCompat(id, title, body) }

        if (!ringing) {
            ringing = true
            AlarmRinger.ensureRinging(applicationContext)
            autoStop.postDelayed({ stopAlarm() }, AUTO_STOP_MILLIS)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        autoStop.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun stopAlarm() {
        autoStop.removeCallbacksAndMessages(null)
        AlarmRinger.stop()
        ringing = false
        AlarmAlert.dismiss(this, activeId)
        stopForegroundCompat()
        stopSelf()
    }

    private fun startForegroundCompat(
        id: Int,
        title: String,
        body: String,
    ) {
        val notification = AlarmAlert.notification(this, id, title, body)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        const val ACTION_DISMISS = "io.github.vedicmitra.core.alarm.action.DISMISS"
        private const val EXTRA_ID = "alarm_id"
        private const val EXTRA_TITLE = "alarm_title"
        private const val EXTRA_BODY = "alarm_body"
        private const val AUTO_STOP_MILLIS = 120_000L

        /** An intent that starts the ringing alarm for [id] with [title] and [body]. */
        fun start(
            context: Context,
            id: Int,
            title: String,
            body: String,
        ): Intent =
            Intent(context, AlarmService::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }

        /** An intent that stops the ringing alarm (from the notification or activity Dismiss). */
        fun dismissIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS)
    }
}
