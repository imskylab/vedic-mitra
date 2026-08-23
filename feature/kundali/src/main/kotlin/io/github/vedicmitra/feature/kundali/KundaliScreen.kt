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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vedicmitra.core.astronomy.ChartYoga
import io.github.vedicmitra.core.astronomy.Graha
import io.github.vedicmitra.core.astronomy.JatakaProfile
import io.github.vedicmitra.core.astronomy.Lagna
import io.github.vedicmitra.core.astronomy.MahadashaPeriod
import io.github.vedicmitra.core.astronomy.Nakshatra
import io.github.vedicmitra.core.astronomy.NatalChart
import io.github.vedicmitra.core.astronomy.NatalGraha
import io.github.vedicmitra.core.astronomy.Rasi
import io.github.vedicmitra.core.designsystem.component.TableColumn
import io.github.vedicmitra.core.designsystem.component.VedicPropertyTable
import io.github.vedicmitra.core.designsystem.component.VedicSelectField
import io.github.vedicmitra.core.designsystem.component.VedicTable
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme
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
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.options.size > 1) {
                ProfilePicker(options = uiState.options, selectedId = uiState.selectedId, onSelect = onSelectProfile)
            }
            Text(text = "${uiState.name}'s Kundali", style = MaterialTheme.typography.titleLarge)
            Text(
                text = pages[pagerState.currentPage].title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            KundaliPageContent(page = pages[page], uiState = uiState)
        }
        PageIndicator(count = pages.size, current = pagerState.currentPage)
    }
}

/** The pages of the kundali book, in swipe order. */
private enum class KundaliPage(
    val title: String,
) {
    LAGNA_CHART("Lagna Kundali"),
    RASHI_CHART("Rashi (Chandra) Kundali"),
    JATAKA("Jataka"),
    SPASHTA_GRAHA("Spashta Graha"),
    YOGAS("Pramukh Yoga"),
    MAHADASHA("Mahadasha"),
    ANTARDASHA("Antardasha"),
    DETAILS("Details"),
}

@Composable
private fun KundaliPageContent(
    page: KundaliPage,
    uiState: KundaliUiState.Ready,
) {
    val chart = uiState.chart
    val scroll = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    when (page) {
        KundaliPage.LAGNA_CHART ->
            ChartPage(
                houses = chart.houses,
                grahas = chart.grahas,
                houseOf = { it.house },
                caption =
                    "North-Indian style. House 1 (top centre) is the lagna; numbers are the rashi " +
                        "in each house. Retrograde grahas are shown in red.",
                modifier = scroll,
            )

        KundaliPage.RASHI_CHART ->
            ChartPage(
                houses = chart.moonHouses,
                grahas = chart.grahas,
                houseOf = { it.houseFromMoon },
                caption =
                    "The same placements counted from the Moon rather than the lagna, so house 1 is " +
                        "the Moon's own rashi. Read alongside the lagna chart.",
                modifier = scroll,
            )

        KundaliPage.JATAKA -> JatakaPage(jataka = chart.jataka, modifier = scroll)

        KundaliPage.SPASHTA_GRAHA -> SpashtaGrahaPage(grahas = chart.grahas, modifier = scroll)

        KundaliPage.YOGAS -> YogasPage(yogas = chart.yogas, modifier = scroll)

        KundaliPage.MAHADASHA -> MahadashaPage(periods = chart.vimshottari, modifier = scroll)

        KundaliPage.ANTARDASHA -> AntardashaPage(periods = chart.vimshottari, modifier = scroll)

        KundaliPage.DETAILS -> DetailsPage(uiState = uiState, modifier = scroll)
    }
}

/** The named combinations found in the chart, each with the placement that produced it. */
@Composable
private fun YogasPage(
    yogas: List<ChartYoga>,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

/** The nine mahadashas of the 120-year cycle, with the running one marked. */
@Composable
private fun MahadashaPage(
    periods: List<MahadashaPeriod>,
    modifier: Modifier,
) {
    val now = remember { System.currentTimeMillis() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            text = "The full Vimshottari cycle from birth. ● marks the period running now.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The nine sub-periods of whichever mahadasha is running now. */
@Composable
private fun AntardashaPage(
    periods: List<MahadashaPeriod>,
    modifier: Modifier,
) {
    val now = remember { System.currentTimeMillis() }
    val running = periods.firstOrNull { isRunning(it.start, it.end, now) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(
            text =
                "Each mahadasha divides into nine antardashas, beginning with its own lord and " +
                    "sharing the period in proportion to each lord's dasha years.",
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

/** The Spashta Graha table: where each graha sits, to the arcminute. */
@Composable
private fun SpashtaGrahaPage(
    grahas: List<NatalGraha>,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VedicTable(
            columns = SPASHTA_COLUMNS,
            rows = grahas.map { spashtaRow(it) },
        )
        Text(
            text =
                "Positions are degrees and minutes into the rashi. Vakri marks retrograde motion. " +
                    "Navamsha is the graha's sign in the D9 division.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val SPASHTA_COLUMNS =
    listOf(
        TableColumn(header = "Graha", weight = 1.1f),
        TableColumn(header = "Position", weight = 1.0f),
        TableColumn(header = "Rashi", weight = 1.1f),
        TableColumn(header = "Nakshatra–pada", weight = 1.6f),
        TableColumn(header = "Navamsha", weight = 1.1f),
    )

private fun spashtaRow(graha: NatalGraha): List<String> =
    listOf(
        if (graha.retrograde) "${graha.graha.displayName} (V)" else graha.graha.displayName,
        "${graha.position.degrees}°${graha.position.minutes.toString().padStart(2, '0')}'",
        graha.rasi.name,
        "${graha.nakshatra.name}–${graha.pada}",
        graha.navamsha.name,
    )

/** Degrees to a whole-degree-and-minute string, matching the Spashta Graha column. */
private fun formatDegrees(degrees: Double): String {
    val totalMinutes = (degrees * MINUTES_PER_DEGREE).toInt()
    return "${totalMinutes / MINUTES_PER_DEGREE}°${(totalMinutes % MINUTES_PER_DEGREE).toString().padStart(2, '0')}'"
}

private const val MINUTES_PER_DEGREE = 60

/** One chart page: the diagram, plus a sentence on how to read it. */
@Composable
private fun ChartPage(
    houses: List<Rasi>,
    grahas: List<NatalGraha>,
    houseOf: (NatalGraha) -> Int,
    caption: String,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NorthIndianChart(houses = houses, grahas = grahas, houseOf = houseOf)
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Everything not yet split into a page of its own — lagna, Moon, dasha and the graha list. */
@Composable
private fun DetailsPage(
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

/** Dots showing which page of the book is open. */
@Composable
private fun PageIndicator(
    count: Int,
    current: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) ACTIVE_DOT else INACTIVE_DOT)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}

private val ACTIVE_DOT = 9.dp
private val INACTIVE_DOT = 7.dp

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
): MahadashaPeriod? =
    chart.vimshottari.firstOrNull {
        nowMillis >= it.start.toEpochMilliseconds() && nowMillis < it.end.toEpochMilliseconds()
    }

private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

private fun formatMonthYear(instant: Instant): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .format(monthYearFormatter)

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
                MahadashaPeriod(
                    lord = Graha.KETU,
                    start = Instant.fromEpochMilliseconds(0L),
                    end = Instant.fromEpochMilliseconds(9_999_999_999_999L),
                ),
            ),
    )
