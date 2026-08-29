/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.cosmicclock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.feature.cosmicclock.domain.ClockRing
import io.github.vedicmitra.feature.cosmicclock.domain.PanchangaClockModel
import io.github.vedicmitra.feature.cosmicclock.domain.spokenSummary
import io.github.vedicmitra.feature.cosmicclock.domain.summaryAt
import io.github.vedicmitra.feature.cosmicclock.ui.ClockHub
import io.github.vedicmitra.feature.cosmicclock.ui.ClockSheet
import io.github.vedicmitra.feature.cosmicclock.ui.LimbList
import io.github.vedicmitra.feature.cosmicclock.ui.PanchangaClock
import io.github.vedicmitra.feature.cosmicclock.ui.clockColors
import io.github.vedicmitra.feature.cosmicclock.ui.formatClockTime
import kotlin.time.Instant

/**
 * The Cosmic Clock — presently one face, the Panchanga clock.
 *
 * The route stays `cosmic-clock` rather than naming this face, so that when a second one lands (the
 * day's windows, then the grahas) the screen gains a selector without any navigation change.
 */
@Composable
fun CosmicClockScreen(
    modifier: Modifier = Modifier,
    viewModel: CosmicClockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    CosmicClockContent(uiState = uiState, now = now, modifier = modifier)
}

@Composable
private fun CosmicClockContent(
    uiState: CosmicClockUiState,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val model = uiState.model
    when {
        uiState.errorMessage != null -> Centered(modifier) { Text(uiState.errorMessage) }
        model == null -> Centered(modifier) { CircularProgressIndicator() }
        else -> ClockBody(model = model, now = now, locationLabel = uiState.locationLabel, modifier = modifier)
    }
}

@Composable
private fun ClockBody(
    model: PanchangaClockModel,
    now: Instant,
    locationLabel: String,
    modifier: Modifier,
) {
    var selected by remember(model) { mutableStateOf<ClockRing?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PanchangaClock(
            model = model,
            progress = model.animatedProgress(),
            colors = clockColors(),
            modifier = Modifier.widthIn(max = MAX_FACE_WIDTH),
            spokenDescription = model.spokenSummary(::formatClockTime),
            onSelectRing = { selected = it },
        ) { hubWidth ->
            model.summaryAt(now)?.let { summary ->
                ClockHub(
                    summary = summary,
                    maxWidth = hubWidth,
                    endsAtLabel = summary.tithiEndsAt?.let { "ends ${formatClockTime(it)}" },
                )
            }
        }
        Text(
            text = locationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LimbList(model = model, at = now, onSelect = { selected = it })
    }
    selected?.let { ring ->
        ClockSheet(ring = ring, at = now, onDismiss = { selected = null })
    }
}

/**
 * Each ring's fill, eased so the arcs move rather than jump.
 *
 * Wrapped in [key] on the active index, and that is load-bearing rather than tidiness. When a limb
 * rolls over, its fraction drops from near 1 to near 0; animating straight to the new value sweeps
 * the arc *backwards* around the whole ring — a full reverse revolution, once per boundary. Changing
 * `key` discards the previous animation state, so the new division starts empty and grows forwards.
 *
 * `animateFloatAsState`'s `label` will not do this: it is for the animation inspector only and has
 * no effect on state identity.
 */
@Composable
private fun PanchangaClockModel.animatedProgress(): List<Float> =
    rings.map { ring ->
        key(ring.concept, ring.activeIndex) {
            val animated by
                animateFloatAsState(
                    targetValue = ring.fraction?.toFloat() ?: 1f,
                    animationSpec = tween(durationMillis = SWEEP_MILLIS, easing = LinearEasing),
                    label = ring.label,
                )
            animated
        }
    }

@Composable
private fun Centered(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** Big enough to read the outer ring's sixty divisions; beyond this the face just wastes space. */
private val MAX_FACE_WIDTH = 340.dp

/** Half a second — visible as movement without asking for a frame every tick. */
private const val SWEEP_MILLIS = 500
