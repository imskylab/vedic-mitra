/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.AstronomySnapshot
import io.github.vedicmitra.core.astronomy.Ayana
import io.github.vedicmitra.core.astronomy.GoldenHour
import io.github.vedicmitra.core.astronomy.Karana
import io.github.vedicmitra.core.astronomy.LimbStep
import io.github.vedicmitra.core.astronomy.Maasa
import io.github.vedicmitra.core.astronomy.MoonPhase
import io.github.vedicmitra.core.astronomy.MoonTimes
import io.github.vedicmitra.core.astronomy.Muhurta
import io.github.vedicmitra.core.astronomy.MuhurtaKind
import io.github.vedicmitra.core.astronomy.MuhurtaQuality
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.Paksha
import io.github.vedicmitra.core.astronomy.PanchangaConcept
import io.github.vedicmitra.core.astronomy.PanchangaLimb
import io.github.vedicmitra.core.astronomy.PanchangaPrimer
import io.github.vedicmitra.core.astronomy.Ritu
import io.github.vedicmitra.core.astronomy.Samvatsara
import io.github.vedicmitra.core.astronomy.SankalpaFrame
import io.github.vedicmitra.core.astronomy.SunTimes
import io.github.vedicmitra.core.astronomy.Tithi
import io.github.vedicmitra.core.astronomy.Vara
import io.github.vedicmitra.core.astronomy.Yoga
import io.github.vedicmitra.core.astronomy.limbSteps
import io.github.vedicmitra.core.astronomy.sankalpaFrame
import io.github.vedicmitra.core.common.model.GeoCoordinates
import io.github.vedicmitra.core.common.model.MaasaReckoning
import io.github.vedicmitra.core.designsystem.component.VedicCycleHeader
import io.github.vedicmitra.core.designsystem.component.VedicCycleRow
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Instant

private const val DAYS_PER_WEEK = 7
private const val SHUKLA_TITHI_COUNT = 15

/**
 * Calendar screen entry point. Resolves the location permission, drives [CalendarViewModel.load],
 * and renders the month grid + selected-day detail. The stateless [CalendarContent] takes state and
 * callbacks as parameters so it is trivially previewable.
 */
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Reload regardless of the result — the ViewModel falls back to a default location.
            viewModel.load()
        }

    LaunchedEffect(Unit) {
        val granted =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.load()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    CalendarContent(
        uiState = uiState,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onSelectDate = viewModel::selectDate,
        modifier = modifier,
    )
}

@Composable
private fun CalendarContent(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        MonthHeader(
            title = uiState.yearMonth.format(monthFormatter),
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )
        WeekdayHeader()
        MonthGrid(
            yearMonth = uiState.yearMonth,
            days = uiState.days,
            selectedDate = uiState.selectedDate,
            today = today,
            onSelectDate = onSelectDate,
        )

        if (uiState.usingDefaultLocation) {
            Text(
                text = "Showing New Delhi — grant location access for your area.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        uiState.selectedSnapshot?.let { snapshot ->
            val festival = uiState.days.firstOrNull { it.date == uiState.selectedDate }?.festival
            DetailCard(
                date = uiState.selectedDate,
                snapshot = snapshot,
                festival = festival,
                reckoning = uiState.maasaReckoning,
                modifier = Modifier.padding(top = 16.dp),
            )
            SankalpaCard(
                frame =
                    snapshot.sankalpaFrame(
                        place = uiState.locationLabel,
                        reckoning = uiState.maasaReckoning,
                    ),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun MonthHeader(
    title: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    // Sunday-first, matching common Hindu panchang layouts.
    val labels = remember { sundayFirstWeekdayLabels() }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    // Sunday-first offset: Sunday(7)->0, Monday(1)->1 .. Saturday(6)->6.
    val leadingBlanks = yearMonth.atDay(1).dayOfWeek.value % DAYS_PER_WEEK
    val totalCells = leadingBlanks + days.size
    val trailingBlanks = (DAYS_PER_WEEK - totalCells % DAYS_PER_WEEK) % DAYS_PER_WEEK
    val cells: List<CalendarDay?> =
        buildList {
            repeat(leadingBlanks) { add(null) }
            addAll(days)
            repeat(trailingBlanks) { add(null) }
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        isToday = day?.date == today,
                        isSelected = day?.date == selectedDate,
                        onSelectDate = onSelectDate,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: CalendarDay?,
    isToday: Boolean,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val base =
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
    if (day == null) {
        Box(modifier = base)
        return
    }

    val shape = MaterialTheme.shapes.small
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val cellModifier =
        base
            .clip(shape)
            .background(background)
            .then(
                if (isToday) {
                    Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
                } else {
                    Modifier
                },
            ).clickable { onSelectDate(day.date) }

    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = day.tithi.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
            )
            if (day.festival != null) {
                // A dot marks a notable day (festival / observance / Sankranti); the detail card names it.
                Box(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun DetailCard(
    date: LocalDate,
    snapshot: AstronomySnapshot,
    festival: String?,
    reckoning: MaasaReckoning,
    modifier: Modifier = Modifier,
) {
    // Tapping a wheel row explains that limb. The copy has existed since 0.9.0 and reached nobody:
    // VedicCycleRow has always taken an onClick, CycleRow never passed one, and the glossary the
    // other detail sheets read is keyed by item name -- it has no entry for "Tithi" at all.
    var explaining by remember { mutableStateOf<PanchangaConcept?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = date.format(dateFormatter), style = MaterialTheme.typography.titleMedium)
            Text(
                text = snapshot.vara.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (festival == null) Modifier.padding(bottom = 8.dp) else Modifier,
            )
            festival?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            // The nine values that repeat on a cycle read as wheels: what each was, is, and becomes.
            // A bare name says what today is called; the neighbours say it is a *sequence*, which is
            // what someone new to a panchanga cannot infer from a table. The rows below -- rise and
            // set times, the muhurtas, and the slow context -- stay as they are, because they are
            // instants and spans rather than positions in a loop.
            val steps = snapshot.limbSteps(reckoning)
            if (steps.isNotEmpty()) {
                VedicCycleHeader(modifier = Modifier.padding(top = 4.dp))
                steps.forEach { step ->
                    CycleRow(step, reckoning) { explaining = step.limb.concept }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            // Ayana and samvatsara stay as table rows: with only two ayanas the neighbour is always
            // the other one, and last year's samvatsara name is trivia. Their *boundaries* are worth
            // showing, which is the half the old rule threw out along with the neighbours.
            DetailRow(
                label = "Samvatsara",
                value = snapshot.samvatsara.name,
                note = snapshot.limbs?.samvatsara?.let { "till ${formatDate(it.end)}" },
            )
            // The three numbered eras a printed panchanga carries on its cover, under the sixty-name
            // cycle they share a boundary with: all of them turn at Ugadi, not at the Gregorian year.
            val eras = snapshot.samvatsara.eras
            DetailRow(
                label = "Samvat",
                value = "Vikrama ${eras.vikrama}",
                note = "Shaka ${eras.shaka} · Kali ${eras.kali}",
            )
            DetailRow(
                label = "Ayana",
                value = snapshot.ayana.displayName,
                note = snapshot.limbs?.ayana?.let { "till ${formatDate(it.end)}" },
            )
            DetailRow(label = "Sunrise", value = formatTime(snapshot.sunTimes.sunrise))
            DetailRow(label = "Sunset", value = formatTime(snapshot.sunTimes.sunset))
            DetailRow(label = "Moonrise", value = formatTime(snapshot.moonTimes.moonrise))
            DetailRow(label = "Moonset", value = formatTime(snapshot.moonTimes.moonset))
            snapshot.muhurtas.forEach { muhurta ->
                DetailRow(
                    label = muhurta.name,
                    value = "${formatTime(muhurta.start)}–${formatTime(muhurta.end)}",
                )
            }
        }
    }
    explaining?.let { concept ->
        PrimerSheet(concept = concept, onDismiss = { explaining = null })
    }
}

/**
 * One primer entry, opened from a tapped wheel row or from the sankalpa card.
 *
 * Both lengths are shown: the one-liner carries a whole idea on its own, and the body is there for
 * the reader who tapped because they wanted more. Nothing here is computed — it is the same copy
 * whatever day is selected — so the sheet takes the concept rather than the snapshot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrimerSheet(
    concept: PanchangaConcept,
    onDismiss: () -> Unit,
) {
    val entry = PanchangaPrimer.of(concept)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = entry.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = entry.oneLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = entry.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Where and when the selected day sits, in the ten measures a sankalpa names.
 *
 * Every value is already on the card above. What this adds is the **order** — and a way to copy the
 * lot out, which is the actual job: someone preparing for an observance otherwise transcribes ten
 * rows off a table by hand and hopes they read the right ones.
 *
 * The one-liner shows without tapping, per the primer's own design: clarity that only arrives on tap
 * is clarity most readers never get.
 */
@Composable
private fun SankalpaCard(
    frame: SankalpaFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var explaining by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sankalpa",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(frame.asText))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(text = "Copy")
                }
            }
            Text(
                text = PanchangaPrimer.of(PanchangaConcept.SANKALPA).oneLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .clickable { explaining = true }
                        .padding(bottom = 8.dp),
            )
            frame.place?.let { DetailRow(label = "Place", value = it) }
            frame.coordinates.forEach { DetailRow(label = it.label, value = it.value) }
        }
    }
    if (explaining) {
        PrimerSheet(concept = PanchangaConcept.SANKALPA, onDismiss = { explaining = false })
    }
}

/**
 * One [LimbStep] as a wheel row.
 *
 * The two neighbour times are the current window's own edges -- the previous value ended when this
 * one began, and the next begins when it ends -- so all three columns come from one window and cost
 * no extra computation.
 *
 * The tithi is shown with its paksha because each tithi name occurs twice a lunar month, and a
 * reader checking against an almanac would otherwise have no way to tell which one this is.
 */
@Composable
private fun CycleRow(
    step: LimbStep,
    reckoning: MaasaReckoning,
    onClick: () -> Unit,
) {
    val paksha = if (step.limb == PanchangaLimb.TITHI) pakshaPrefix(step.position) else ""
    // The month row carries its scheme in the label. A reader whose almanac disagrees needs to know
    // which convention this is, and that reader would never think to look in Settings for it.
    val label =
        if (step.limb == PanchangaLimb.MAASA) {
            "${step.limb.displayName} · ${reckoning.shortLabel}"
        } else {
            step.limb.displayName
        }
    VedicCycleRow(
        label = label,
        previous = step.previous,
        current = "$paksha${step.current}",
        next = step.next,
        previousNote = step.window?.let { formatTime(it.start) },
        currentNote = step.window?.let { "till ${formatTime(it.end)}" },
        nextNote = step.window?.let { formatTime(it.end) },
        progress = step.fraction?.toFloat(),
        spokenDescription = cycleRowDescription(step, paksha),
        onClick = onClick,
    )
}

/** "Shukla " or "Krishna ", from the tithi's place in the month. Empty for every other limb. */
private fun pakshaPrefix(tithiPosition: Int): String =
    if (tithiPosition <= SHUKLA_TITHI_COUNT) "Shukla " else "Krishna "

/** The row as one sentence, so a screen reader hears it whole rather than as seven fragments. */
private fun cycleRowDescription(
    step: LimbStep,
    paksha: String,
): String {
    val window = step.window
    val ending = if (window == null) "" else ", until ${formatTime(window.end)}"
    val after = if (window == null) "" else ", when ${step.next} begins"
    return "${step.limb.displayName}: $paksha${step.current}$ending$after. " +
        "Follows ${step.previous}."
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    note: String? = null,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * When a limb gives way to the next, as an absolute clock time.
 *
 * Absolute rather than a countdown on purpose: this card describes a chosen day, computed at that
 * day's sunrise, so "ends in 4h" would be meaningless for any day but today.
 */
private val Tithi.shortLabel: String
    get() {
        val inPaksha = if (number <= SHUKLA_TITHI_COUNT) number else number - SHUKLA_TITHI_COUNT
        val pakshaLetter = if (paksha == Paksha.SHUKLA) "S" else "K"
        return "$pakshaLetter$inPaksha"
    }

private val MaasaReckoning.shortLabel: String
    get() =
        when (this) {
            MaasaReckoning.AMANTA -> "Amanta"
            MaasaReckoning.PURNIMANTA -> "Purnimanta"
        }

private val boundaryFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Sunday-first short weekday labels ("Sun".."Sat") in the device locale. */
private fun sundayFirstWeekdayLabels(): List<String> =
    (0 until DAYS_PER_WEEK).map { offset ->
        java.time.DayOfWeek.SUNDAY
            .plus(offset.toLong())
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

/** A day and month, for boundaries that are weeks or months away rather than hours. */
private fun formatDate(instant: Instant?): String {
    if (instant == null) return "—"
    return java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(boundaryFormatter)
}

/** Formats an instant as local wall-clock time in the device's zone, or an em dash if absent. */
private fun formatTime(instant: Instant?): String {
    if (instant == null) return "—"
    val local =
        java.time.Instant
            .ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(ZoneId.systemDefault())
    return local.format(timeFormatter)
}

@Preview
@Composable
private fun CalendarContentPreview() {
    val month = YearMonth.of(2026, 8)
    val days =
        (1..month.lengthOfMonth()).map { day ->
            CalendarDay(
                date = month.atDay(day),
                tithi = Tithi(number = (day % 30) + 1, paksha = Paksha.SHUKLA, name = "Panchami"),
                moonPhase = MoonPhase.WAXING_GIBBOUS,
                festival =
                    when (day) {
                        5 -> "Ganesh Chaturthi"
                        15 -> "Purnima"
                        else -> null
                    },
            )
        }
    val sample =
        AstronomySnapshot(
            instant = Instant.fromEpochMilliseconds(1_785_911_400_000L),
            location = GeoCoordinates(latitude = 28.6139, longitude = 77.2090),
            sunTimes =
                SunTimes(
                    sunrise = Instant.fromEpochMilliseconds(1_785_888_000_000L),
                    sunset = Instant.fromEpochMilliseconds(1_785_934_800_000L),
                ),
            moonTimes =
                MoonTimes(
                    moonrise = Instant.fromEpochMilliseconds(1_785_951_240_000L),
                    moonset = Instant.fromEpochMilliseconds(1_785_910_920_000L),
                ),
            tithi = Tithi(number = 22, paksha = Paksha.KRISHNA, name = "Saptami"),
            nakshatra = Nakshatra(number = 1, name = "Ashwini"),
            yoga = Yoga(number = 9, name = "Shula"),
            karana = Karana(number = 44, name = "Bava"),
            vara = Vara.BUDHAVARA,
            maasa = Maasa(number = 4, name = "Ashadha", adhika = false),
            samvatsara = Samvatsara(number = 40, name = "Parabhava", shakaYear = 1948),
            ayana = Ayana.DAKSHINAYANA,
            ritu = Ritu.VARSHA,
            moonPhase = MoonPhase.WANING_CRESCENT,
            goldenHour = GoldenHour(morningStart = null, morningEnd = null, eveningStart = null, eveningEnd = null),
            muhurtas =
                listOf(
                    Muhurta(
                        kind = MuhurtaKind.BRAHMA,
                        name = "Brahma Muhurta",
                        start = Instant.fromEpochMilliseconds(1_785_884_400_000L),
                        end = Instant.fromEpochMilliseconds(1_785_887_280_000L),
                        quality = MuhurtaQuality.AUSPICIOUS,
                    ),
                ),
        )
    VedicMitraTheme {
        CalendarContent(
            uiState =
                CalendarUiState(
                    yearMonth = month,
                    selectedDate = month.atDay(5),
                    days = days,
                    selectedSnapshot = sample,
                    isLoading = false,
                ),
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDate = {},
        )
    }
}
