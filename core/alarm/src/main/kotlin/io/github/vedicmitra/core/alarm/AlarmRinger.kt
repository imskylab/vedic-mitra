/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

@file:Suppress("MagicNumber")

package io.github.vedicmitra.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays the system alarm ringtone on a loop (on the alarm audio stream, so it bypasses ringer mute
 * and, by default, Do Not Disturb) and vibrates, until [stop]. Failures to obtain or play the tone
 * are swallowed so the alarm UI still shows.
 */
internal class AlarmRinger(
    private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun start() {
        startSound()
        startVibration()
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            p.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun startSound() {
        val uri =
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: return
        runCatching {
            player =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
        }
    }

    private fun startVibration() {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
        this.vibrator = vibrator
        // Buzz 800ms, pause 600ms, repeated from index 0.
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 800L, 600L), 0))
    }
}
