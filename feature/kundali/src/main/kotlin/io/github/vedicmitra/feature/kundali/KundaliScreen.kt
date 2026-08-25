/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.kundali

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.Ashtakavarga
import io.github.vedicmitra.core.astronomy.DashaPeriod
import io.github.vedicmitra.core.astronomy.DashaSystem
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.JatakaProfile
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.MangalDosha
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.NatalGraha
import io.github.vedicmitra.core.astronomy.RASHI_NAMES
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.astronomy.Varga
import io.github.vedicmitra.core.astronomy.mangalDoshaOf
import io.github.vedicmitra.core.astronomy.vargaChart
import io.github.vedicmitra.core.designsystem.component.TableColumn
import io.github.vedicmitra.core.designsystem.component.VedicPropertyTable
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.component.VedicTable
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

/**
 * Kundali screen. Loads the primary profile's [NatalChart] via [KundaliViewModel] and renders it, or
 * prompts to set up a birth profile. The stateless [KundaliContent] is previewable and testable.
 */
@Composable
fun KundaliScreen(
    onSetUpProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KundaliViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    KundaliContent(
        uiState = uiState,
        onSetUpProfile = onSetUpProfile,
        onSelectProfile = viewModel::select,
        modifier = modifier,
    )
}

@Composable
private fun KundaliContent(
    uiState: KundaliUiState,
    onSetUpProfile: () -> Unit,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        KundaliUiState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        KundaliUiState.NeedsProfile -> NeedsProfile(onSetUpProfile, modifier)
        is KundaliUiState.Ready -> ChartView(uiState, onSelectProfile, modifier)
    }
}

@Composable
private fun NeedsProfile(
    onSetUpProfile: () -> Unit,
    modifier: Modifier,
) {
    Centered(modifier) {
        Text(
            text = "Add your date, exact time and place of birth to see your kundali.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSetUpProfile) { Text("Set up profile") }
    }
}

@Composable
private fun ChartView(
    uiState: KundaliUiState.Ready,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier,
) {
    val pages = KundaliPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.options.size > 1) {
                ProfilePicker(options = uiState.options, selectedId = uiState.selectedId, onSelect = onSelectProfile)
            }
            Text(text = "${uiState.name}'s Kundali", style = MaterialTheme.typography.titleLarge)
        }
        // Named tabs rather than dots. Ten identical dots told a reader where they were but never
        // where anything else was, so reaching the last section meant swiping past every other one.
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
        ) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = index == pagerState.currentPage,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = page.title) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            KundaliPageContent(page = pages[page], uiState = uiState)
        }
    }
}

/**
 * The sections of the kundali book, in swipe order.
 *
 * Grouped by the question a reader is asking rather than by calculation. The book had grown to ten
 * pages one addition at a time, which is how it ended up with the mahadasha and its antardashas on
 * separate pages, and a graha's position two swipes from its bindu support. Six named sections fit a
 * tab row, and the things read together now sit together.
 */
private enum class KundaliPage(
    val title: String,
) {
    CHARTS("Charts"),
    JATAKA("Jataka"),
    GRAHAS("Grahas"),
    YOGAS("Yogas"),
    DASHA("Dasha"),
    READING("Reading"),
}

@Composable
private fun KundaliPageContent(
    page: KundaliPage,
    uiState: KundaliUiState.Ready,
) {
    val chart = uiState.chart
    val scroll = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    when (page) {
        KundaliPage.CHARTS -> ChartsPage(chart = chart, modifier = scroll)

        KundaliPage.JATAKA -> JatakaPage(jataka = chart.jataka, modifier = scroll)

        KundaliPage.GRAHAS -> GrahasPage(chart = chart, modifier = scroll)

        KundaliPage.YOGAS -> YogasPage(chart = chart, modifier = scroll)

        KundaliPage.DASHA -> DashaPage(chart = chart, modifier = scroll)

        KundaliPage.READING -> ReadingPage(uiState = uiState, modifier = scroll)
    }
}

/**
 * Ashtakavarga: how much benefic support each sign carries.
 *
 * The sarva row is the one a reader uses — a transit through a sign holding 30 bindus is read very
 * differently from the same transit through one holding 20 — so it leads, with the seven grahas'
 * own rows beneath it for anyone who wants to see where the support comes from.
 */
@Composable
private fun AshtakavargaSection(chart: NatalChart) {
    val sarva = chart.sarvashtakavarga
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Sarvashtakavarga",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        VedicTable(
            columns = ASHTAKAVARGA_COLUMNS,
            rows =
                sarva.mapIndexed { index, bindus ->
                    listOf(RASHI_NAMES[index], bindus.toString(), strengthLabel(bindus))
                },
        )
        Text(
            text = "Total ${sarva.sum()} — always 337, spread over the twelve signs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Binnashtakavarga",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        VedicTable(
            columns = BINNA_COLUMNS,
            rows =
                RASHI_NAMES.mapIndexed { sign, name ->
                    listOf(name) +
                        Ashtakavarga.CONTRIBUTORS.map { chart.binnashtakavarga(it)[sign].toString() }
                },
        )
        Text(
            text =
                "Each graha marks certain houses from every reference point as benefic, and a sign " +
                    "collects one bindu per reference point that marks it — so 0 to 8 in each " +
                    "column. Rahu and Ketu take no part.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Rough bands for the sarva row; 337 over twelve signs makes the average a little over 28. */
private fun strengthLabel(bindus: Int): String =
    when {
        bindus >= STRONG_BINDUS -> "Strong"
        bindus >= GOOD_BINDUS -> "Good"
        bindus >= MIDDLING_BINDUS -> "Middling"
        else -> "Weak"
    }

private const val STRONG_BINDUS = 32
private const val GOOD_BINDUS = 28
private const val MIDDLING_BINDUS = 25

private val ASHTAKAVARGA_COLUMNS =
    listOf(
        TableColumn(header = "Rashi", weight = 1.4f),
        TableColumn(header = "Bindus", weight = 0.8f),
        TableColumn(header = "", weight = 1.0f),
    )

private val BINNA_COLUMNS =
    listOf(TableColumn(header = "Rashi", weight = 1.6f)) +
        listOf("Su", "Mo", "Ma", "Me", "Ju", "Ve", "Sa").map {
            TableColumn(header = it, weight = 0.6f)
        }

/**
 * Which dasha system the timeline is read in.
 *
 * Vimshottari first because it is what a reader means by "dasha" unless they say otherwise; the
 * other two are read alongside it rather than instead of it, which is why this is a switch on one
 * page rather than three more pages.
 */
@Composable
private fun DashaSystemChips(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashaSystem.entries.forEachIndexed { index, system ->
            FilterChip(
                selected = index == selected,
                onClick = { onSelect(index) },
                label = { Text(text = system.displayName) },
            )
        }
    }
}

/**
 * Mangal dosha for this chart, with every trigger and every parihara shown.
 *
 * The working is the whole point. Most charts trigger the rule somewhere, so a bare "Manglik" would
 * alarm almost everyone and inform no one; what a reader needs is which placement raised it and
 * whether anything answers it. Shown on the yoga page because a dosha is a combination like any
 * other, and read here for one chart rather than for a pair — Kundali Matching handles the pair.
 */
@Composable
private fun MangalDoshaCard(dosha: MangalDosha) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Mangal dosha",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (dosha.present) "Present" else "Not standing",
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (dosha.present) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        if (!dosha.afflicted) {
            Text(
                text = "Mars falls in no house that raises the dosha, from the lagna, the Moon or Venus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        dosha.triggers.forEach { trigger ->
            Text(
                text = trigger.description + (trigger.cancellation?.let { " — $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        dosha.cancellations.forEach { cancellation ->
            Text(
                text = cancellation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The named combinations found in the chart, each with the placement that produced it. */
@Composable
private fun YogasPage(
    chart: NatalChart,
    modifier: Modifier,
) {
    val yogas = chart.yogas
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MangalDoshaCard(dosha = mangalDoshaOf(chart))
        if (yogas.isEmpty()) {
            Text(
                text = "None of the combinations this app checks for are present in this chart.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        yogas.forEach { yoga ->
            InfoCard(label = yoga.name, value = yoga.rule, detail = yoga.summary)
        }
        Text(
            text =
                "A deliberately short list — only combinations that follow from where the grahas " +
                    "sit, so each can be checked against the chart pages. Yogas needing aspects or " +
                    "divisional charts are not computed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The whole dasha timeline: the major periods, then the one running now opened out.
 *
 * Mahadasha and antardasha were separate pages, which meant the period you are in and the period
 * inside it were a swipe apart -- and the reader nearly always wants both at once. One page, one
 * system selector, three levels deep.
 */
@Composable
private fun DashaPage(
    chart: NatalChart,
    modifier: Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(DashaSystem.VIMSHOTTARI.ordinal) }
    val system = DashaSystem.entries[selected]
    val periods = chart.dasha(system)
    val now = remember { System.currentTimeMillis() }
    val running = periods.firstOrNull { isRunning(it.start, it.end, now) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashaSystemChips(selected = selected, onSelect = { selected = it })
        VedicTable(
            columns = DASHA_COLUMNS,
            rows =
                periods.map { period ->
                    listOf(
                        period.lord.displayName + if (isRunning(period.start, period.end, now)) " ●" else "",
                        formatMonthYear(period.start),
                        formatMonthYear(period.end),
                    )
                },
        )
        Text(
            text = "The full ${system.displayName} cycle from birth. ● marks the period running now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (running == null) {
            Text(
                text = "No mahadasha covers the present moment, so there are no sub-periods to show.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Text(
            text = "Within ${running.lord.displayName} mahadasha",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        VedicTable(
            columns = DASHA_COLUMNS,
            rows =
                running.antardashas.map { period ->
                    listOf(
                        period.lord.displayName + if (isRunning(period.start, period.end, now)) " ●" else "",
                        formatMonthYear(period.start),
                        formatMonthYear(period.end),
                    )
                },
        )
        val runningAntardasha = running.antardashas.firstOrNull { isRunning(it.start, it.end, now) }
        if (runningAntardasha != null) {
            Text(
                text = "Within ${runningAntardasha.lord.displayName} antardasha",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            VedicTable(
                columns = DASHA_COLUMNS,
                rows =
                    runningAntardasha.subPeriods.map { period ->
                        listOf(
                            period.lord.displayName + if (isRunning(period.start, period.end, now)) " ●" else "",
                            formatDayMonthYear(period.start),
                            formatDayMonthYear(period.end),
                        )
                    },
            )
        }
        Text(
            text =
                "Each mahadasha divides into antardashas beginning with its own lord, sharing the " +
                    "period in proportion to each lord's dasha years. The same division applied " +
                    "once more gives the pratyantardashas — a few weeks each, so those carry the " +
                    "day as well as the month.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val DASHA_COLUMNS =
    listOf(
        TableColumn(header = "Lord", weight = 1.2f),
        TableColumn(header = "From", weight = 1f),
        TableColumn(header = "To", weight = 1f),
    )

private fun isRunning(
    start: Instant,
    end: Instant,
    nowMillis: Long,
): Boolean = nowMillis >= start.toEpochMilliseconds() && nowMillis < end.toEpochMilliseconds()

/** The jataka's standing properties — the block a printed panchanga sets beside the charts. */
@Composable
private fun JatakaPage(
    jataka: JatakaProfile?,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (jataka == null) {
            Text(
                text = "Birth details are incomplete, so these properties cannot be derived.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        VedicPropertyTable(entries = jatakaEntries(jataka))
        Text(
            text =
                "Gana, varna, yoni and nadi are the same classifications used for Ashtakoota " +
                    "matching. The sun sign is the Western tropical one; every other value here is " +
                    "sidereal, and the ayanamsa is the difference between the two.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun jatakaEntries(jataka: JatakaProfile): List<Pair<String, String>> =
    listOf(
        "Janma Rashi" to jataka.janmaRashi.name,
        "Rashi lord" to jataka.rashiLord.displayName,
        "Nakshatra" to "${jataka.nakshatra.name} · pada ${jataka.pada}",
        // In the classical Ashtakoota order, which is how a panchanga lists them.
        "Varna" to jataka.varna,
        "Vashya" to jataka.vashya,
        "Yoni" to jataka.yoni,
        "Gana" to jataka.gana,
        "Nadi" to jataka.nadi,
        "Lagna" to jataka.lagna.name,
        "Surya Rashi" to jataka.sunRashi.name,
        "Sun sign (Western)" to jataka.sunSign,
        "Ayanamsa" to formatDegrees(jataka.ayanamsa),
        "Shaka Samvat" to jataka.shakaSamvat.toString(),
        "Vikram Samvat" to jataka.vikramSamvat.toString(),
        "Samvatsara" to jataka.samvatsara,
    )

/**
 * Where each graha sits, and how much benefic support each sign carries.
 *
 * Position and bindus on one screen because they are read together: a graha's house means one thing
 * in a sign holding 30 bindus and another in a sign holding 20. They used to be two swipes apart.
 */
@Composable
private fun GrahasPage(
    chart: NatalChart,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Spashta Graha", style = MaterialTheme.typography.titleSmall)
        VedicTable(columns = SPASHTA_COLUMNS, rows = chart.grahas.map { spashtaRow(it) })
        Text(
            text =
                "Positions are degrees and minutes into the rashi. (V) marks retrograde motion and " +
                    "A marks astangata — combust, lost in the Sun's glare, by the classical orbs. " +
                    "Navamsha is the graha's sign in the D9 division.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AshtakavargaSection(chart = chart)
    }
}

private val SPASHTA_COLUMNS =
    listOf(
        TableColumn(header = "Graha", weight = 1.1f),
        TableColumn(header = "Position", weight = 1.0f),
        TableColumn(header = "Rashi", weight = 1.1f),
        TableColumn(header = "Nakshatra–pada", weight = 1.5f),
        TableColumn(header = "Navamsha", weight = 1.0f),
        TableColumn(header = "Ast.", weight = 0.5f),
    )

private fun spashtaRow(graha: NatalGraha): List<String> =
    listOf(
        if (graha.retrograde) "${graha.graha.displayName} (V)" else graha.graha.displayName,
        "${graha.position.degrees}°${graha.position.minutes.toString().padStart(2, '0')}'",
        graha.rasi.name,
        "${graha.nakshatra.name}–${graha.pada}",
        graha.navamsha.name,
        if (graha.combust) "A" else "",
    )

/** Degrees to a whole-degree-and-minute string, matching the Spashta Graha column. */
private fun formatDegrees(degrees: Double): String {
    val totalMinutes = (degrees * MINUTES_PER_DEGREE).toInt()
    return "${totalMinutes / MINUTES_PER_DEGREE}°${(totalMinutes % MINUTES_PER_DEGREE).toString().padStart(2, '0')}'"
}

private const val MINUTES_PER_DEGREE = 60

/**
 * Every figure of the chart in one place: the lagna chart, the same placements read from the Moon,
 * and the sixteen divisional charts.
 *
 * One chip row rather than three pages, because they are the same diagram drawn from different
 * starting points and a reader compares them against each other. The D-1 chip that used to sit at
 * the head of the varga list is gone: it was the lagna chart under another name, which was worth
 * saying when the two lived on separate pages and is just a duplicate now they do not.
 */
@Composable
private fun ChartsPage(
    chart: NatalChart,
    modifier: Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(0) }
    val figures = chartFigures(chart)
    val figure = figures[selected.coerceIn(figures.indices)]
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            figures.forEachIndexed { index, entry ->
                FilterChip(
                    selected = index == selected,
                    onClick = { selected = index },
                    label = { Text(text = entry.chip) },
                )
            }
        }
        Text(
            text = figure.heading,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        NorthIndianChart(houses = figure.houses, grahas = chart.grahas, houseOf = figure.houseOf)
        Text(
            text = figure.caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One selectable figure: what its chip says, what it draws, and how to read it. */
private data class ChartFigure(
    val chip: String,
    val heading: String,
    val houses: List<Rasi>,
    val houseOf: (NatalGraha) -> Int,
    val caption: String,
)

/** The lagna and Chandra figures, then every varga but D-1, which the lagna figure already is. */
private fun chartFigures(chart: NatalChart): List<ChartFigure> =
    listOf(
        ChartFigure(
            chip = "Lagna",
            heading = "Lagna Kundali",
            houses = chart.houses,
            houseOf = { it.house },
            caption =
                "North-Indian style. House 1 (top centre) is the lagna; numbers are the rashi in " +
                    "each house. Retrograde grahas are shown in red.",
        ),
        ChartFigure(
            chip = "Chandra",
            heading = "Rashi (Chandra) Kundali",
            houses = chart.moonHouses,
            houseOf = { it.houseFromMoon },
            caption =
                "The same placements counted from the Moon rather than the lagna, so house 1 is " +
                    "the Moon's own rashi. Read alongside the lagna chart.",
        ),
    ) +
        Varga.entries.filter { it != Varga.D1 }.map { varga ->
            val cast = chart.vargaChart(varga)
            ChartFigure(
                chip = varga.name,
                heading = "${varga.name} · ${varga.displayName} — ${VARGA_PURPOSE[varga].orEmpty()}",
                houses = cast.houses,
                houseOf = cast::houseOf,
                caption = vargaCaption(varga),
            )
        }

/** How to read the divisional figure on screen, and what not to read into it. */
private fun vargaCaption(varga: Varga): String =
    if (varga == Varga.D1) {
        "The rashi chart itself — the same figure as the Lagna Kundali, kept here so the divisions " +
            "can be read against the chart they came from."
    } else {
        val stepNote =
            if (varga.step == 1) {
                ""
            } else {
                " Each successive part moves ${varga.step} signs on, not one."
            }
        val precisionNote =
            if (varga.needsExactBirthTime) {
                " At this width the birth time matters more than the arithmetic: the ascendant " +
                    "moves a degree in about four minutes, so a time known only to the nearest five " +
                    "minutes leaves this chart's houses uncertain."
            } else {
                " Positions are good to about an arcminute, so a graha sitting on a division edge " +
                    "may fall either side of it."
            }
        "Each rashi is cut into ${varga.divisions} equal parts of " +
            "${formatDegrees(DEGREES_PER_RASHI / varga.divisions)} each, and every graha takes the " +
            "sign its part belongs to.$stepNote House 1 is the lagna's own sign in this division, " +
            "not in the rashi chart, so the houses mean here what they mean anywhere.$precisionNote"
    }

private const val DEGREES_PER_RASHI = 30.0

/** What each divisional chart is traditionally read for. */
private val VARGA_PURPOSE: Map<Varga, String> =
    mapOf(
        Varga.D1 to "the birth chart itself; body, life and everything else in outline",
        Varga.D3 to "siblings, courage and one's own initiative",
        Varga.D4 to "property, fixed assets and the home",
        Varga.D6 to "illness, debts and the troubles one contends with",
        Varga.D7 to "children and lineage",
        Varga.D8 to "sudden difficulty, longevity and what is inherited",
        Varga.D9 to "marriage, the partner, and the inner strength of every graha",
        Varga.D10 to "career, standing and what one is known for",
        Varga.D11 to "gains, and the undoing of them",
        Varga.D12 to "parents and what came before",
        Varga.D16 to "vehicles, comforts and happiness of the ordinary kind",
        Varga.D20 to "worship, practice and spiritual inclination",
        Varga.D24 to "learning, and the fruits of study",
        Varga.D27 to "strength and weakness in the body's own constitution",
        Varga.D40 to "what is inherited through the mother's line",
        Varga.D45 to "what is inherited through the father's line",
        Varga.D60 to
            "the whole of the chart in miniature — weighted heavily, and the most demanding of an exact birth time",
    )

/**
 * The chart in plain language — the one section here that interprets rather than tabulates.
 *
 * It was called "Details" and sat last, which undersold it: every other section is a diagram or a
 * table, and this is the only place the app says what any of it is taken to mean.
 */
@Composable
private fun ReadingPage(
    uiState: KundaliUiState.Ready,
    modifier: Modifier,
) {
    val chart = uiState.chart
    val now = remember { System.currentTimeMillis() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            label = "Lagna (Ascendant)",
            value = chart.lagna.rasi.name,
            detail = KundaliSignificance.lagna(chart.lagna.rasi.index),
        )
        InfoCard(
            label = "Moon",
            value = "${chart.moonNakshatra.name} · pada ${chart.moonPada}",
            detail = KundaliSignificance.moon(chart.moonNakshatra.number),
        )
        currentDashaOf(chart, now)?.let {
            InfoCard(
                label = "Current dasha",
                value = "${it.lord.displayName} · until ${formatMonthYear(it.end)}",
                detail = KundaliSignificance.dasha(it.lord),
            )
        }
        Text(text = "Grahas", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Tap a placement for its significance.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        chart.grahas.forEach { GrahaRow(it) }
    }
}

/** A dropdown to pick which chart-ready profile's kundali to show. */
@Composable
private fun ProfilePicker(
    options: List<KundaliProfileOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    VedicSelectField(
        label = "Profile",
        options = options,
        selected = options.firstOrNull { it.id == selectedId } ?: options.first(),
        optionLabel = { it.name },
        onSelect = { onSelect(it.id) },
    )
}

/**
 * A North-Indian style chart: a fixed square divided by both diagonals and the diamond joining the
 * side midpoints, giving twelve houses. House 1 is the top-centre triangle and the rest run
 * anticlockwise. Each house shows the number of the rashi that falls in it and the grahas placed
 * there (retrograde in the error colour).
 *
 * Takes [houses] and a [houseOf] selector rather than a whole chart, so one diagram draws both the
 * lagna framing and the Chandra framing without a second copy of the geometry.
 */
@Composable
private fun NorthIndianChart(
    houses: List<Rasi>,
    grahas: List<NatalGraha>,
    houseOf: (NatalGraha) -> Int,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.outline
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val s = 1.5.dp.toPx()
            drawRect(color = line, style = Stroke(width = s))
            drawLine(line, Offset(0f, 0f), Offset(w, h), s)
            drawLine(line, Offset(w, 0f), Offset(0f, h), s)
            drawLine(line, Offset(w / 2f, 0f), Offset(w, h / 2f), s)
            drawLine(line, Offset(w, h / 2f), Offset(w / 2f, h), s)
            drawLine(line, Offset(w / 2f, h), Offset(0f, h / 2f), s)
            drawLine(line, Offset(0f, h / 2f), Offset(w / 2f, 0f), s)
        }
        HOUSE_ANCHORS.forEachIndexed { index, anchor ->
            HouseCell(
                rasi = houses.getOrNull(index),
                grahas = grahas.filter { houseOf(it) == index + 1 },
                modifier = Modifier.align(BiasAlignment(anchor.first, anchor.second)),
            )
        }
    }
}

/** One house of the chart: the rashi number above the grahas placed in that house. */
@Composable
private fun HouseCell(
    rasi: Rasi?,
    grahas: List<NatalGraha>,
    modifier: Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (rasi != null) {
            Text(
                text = "${rasi.index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        grahas.forEach { graha ->
            Text(
                text = graha.graha.shortLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (graha.retrograde) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Horizontal/vertical bias (each in -1..1) that centres each house's label at its centroid. */
private val HOUSE_ANCHORS: List<Pair<Float, Float>> =
    listOf(
        0.0f to -0.40f, // 1 — top centre (lagna)
        -0.50f to -0.76f, // 2 — top left
        -0.76f to -0.50f, // 3 — left upper
        -0.44f to 0.0f, // 4 — left centre
        -0.76f to 0.50f, // 5 — left lower
        -0.50f to 0.76f, // 6 — bottom left
        0.0f to 0.40f, // 7 — bottom centre
        0.50f to 0.76f, // 8 — bottom right
        0.76f to 0.50f, // 9 — right lower
        0.44f to 0.0f, // 10 — right centre
        0.76f to -0.50f, // 11 — right upper
        0.50f to -0.76f, // 12 — top right
    )

/** Two-letter code used inside the chart cells. */
private val Graha.shortLabel: String
    get() =
        when (this) {
            Graha.SUN -> "Su"
            Graha.MOON -> "Mo"
            Graha.MANGALA -> "Ma"
            Graha.BUDHA -> "Me"
            Graha.GURU -> "Ju"
            Graha.SHUKRA -> "Ve"
            Graha.SHANI -> "Sa"
            Graha.RAHU -> "Ra"
            Graha.KETU -> "Ke"
        }

@Composable
private fun InfoCard(
    label: String,
    value: String,
    detail: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (detail != null) Modifier.clickable { expanded = !expanded } else Modifier),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
            if (detail != null && expanded) {
                Text(text = detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GrahaRow(graha: NatalGraha) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = graha.graha.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(text = graha.rasi.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = "House ${graha.house}" + if (graha.retrograde) " · R" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Text(
                text =
                    KundaliSignificance.grahaInHouse(
                        graha = graha.graha,
                        rasiName = graha.rasi.name,
                        house = graha.house,
                        retrograde = graha.retrograde,
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Centered(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun currentDashaOf(
    chart: NatalChart,
    nowMillis: Long,
): DashaPeriod? =
    chart.vimshottari.firstOrNull {
        nowMillis >= it.start.toEpochMilliseconds() && nowMillis < it.end.toEpochMilliseconds()
    }

private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

private fun formatMonthYear(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(monthYearFormatter)

/** Pratyantardashas run a few weeks, so a month alone would show the same value on several rows. */
private val dayMonthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatDayMonthYear(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(dayMonthYearFormatter)

@Preview
@Composable
private fun KundaliContentPreview() {
    VedicMitraTheme {
        KundaliContent(
            uiState =
                KundaliUiState.Ready(
                    name = "Leo",
                    chart = sampleChart(),
                    selectedId = "a",
                    options =
                        listOf(
                            KundaliProfileOption(id = "a", name = "Leo"),
                            KundaliProfileOption(id = "b", name = "Mia"),
                        ),
                ),
            onSetUpProfile = {},
            onSelectProfile = {},
        )
    }
}

@Suppress("MagicNumber")
private fun sampleChart(): NatalChart =
    NatalChart(
        lagna = Lagna(siderealLongitude = 15.0, rasi = Rasi(0, "Mesha")),
        houses = (0 until 12).map { Rasi(it, "Rashi ${it + 1}") },
        // The Moon sits in Vrishabha, so the Chandra framing starts one sign later.
        moonHouses = (0 until 12).map { Rasi((it + 1) % 12, "Rashi ${(it + 1) % 12 + 1}") },
        grahas =
            listOf(
                NatalGraha(Graha.SUN, 10.0, Rasi(0, "Mesha"), house = 1, houseFromMoon = 12, retrograde = false),
                NatalGraha(Graha.MOON, 42.0, Rasi(1, "Vrishabha"), house = 2, houseFromMoon = 1, retrograde = false),
                NatalGraha(Graha.SHANI, 195.0, Rasi(6, "Tula"), house = 7, houseFromMoon = 6, retrograde = true),
            ),
        moonNakshatra = Nakshatra(number = 3, name = "Krittika"),
        moonPada = 2,
        vimshottari =
            listOf(
                DashaPeriod(
                    lord = Graha.KETU,
                    start = Instant.fromEpochMilliseconds(0L),
                    end = Instant.fromEpochMilliseconds(9_999_999_999_999L),
                    level = 1,
                ),
            ),
    )
