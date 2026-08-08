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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Full-screen alarm shown when an alarm-mode muhurta reminder fires. It surfaces over the lock
 * screen, turns the screen on, and plays the ringtone/vibration (via [AlarmRinger]) until the user
 * taps Dismiss or a safety timeout elapses.
 */
class AlarmActivity : ComponentActivity() {
    private var ringer: AlarmRinger? = null
    private val autoStop = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        ringer = AlarmRinger(applicationContext).also { it.start() }
        autoStop.postDelayed({ dismiss(id) }, AUTO_STOP_MILLIS)

        setContent {
            VedicMitraTheme {
                AlarmContent(title = title, body = body, onDismiss = { dismiss(id) })
            }
        }
    }

    override fun onDestroy() {
        autoStop.removeCallbacksAndMessages(null)
        ringer?.stop()
        ringer = null
        super.onDestroy()
    }

    private fun dismiss(id: Int) {
        AlarmAlert.dismiss(this, id)
        finish()
    }

    @Suppress("DEPRECATION")
    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
    }

    companion object {
        private const val EXTRA_ID = "alarm_id"
        private const val EXTRA_TITLE = "alarm_title"
        private const val EXTRA_BODY = "alarm_body"
        private const val AUTO_STOP_MILLIS = 120_000L

        /** An intent that launches the full-screen alarm for [id] with [title] and [body]. */
        fun intent(
            context: Context,
            id: Int,
            title: String,
            body: String,
        ): Intent =
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
                // NEW_TASK (required from a non-activity context) + SINGLE_TOP to reuse an existing
                // alarm screen. Deliberately NOT CLEAR_TASK: that tore down the app's own task, so a
                // firing alarm would close the running app.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}

@Composable
private fun AlarmContent(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
                Text(text = "Dismiss")
            }
        }
    }
}
