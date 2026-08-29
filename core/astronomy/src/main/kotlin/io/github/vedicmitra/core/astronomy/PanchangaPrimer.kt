/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.astronomy

/**
 * Plain-language explanations of the panchanga *ideas*, for a reader who has never met them.
 *
 * ## Why this is not [PanchangaGlossary]
 *
 * That object explains **named items** — "Rahu Kalam", "Ekadashi", "Diwali" — keyed by the exact
 * string each is displayed with, and returns `null` for anything it does not know. That shape is
 * right for items and wrong for concepts, in two ways. Concept names collide with item names
 * ("Purnima" is both a tithi and an observance), and a missing key degrades silently to a fallback
 * string rather than failing loudly.
 *
 * So concepts are keyed by [PanchangaConcept] instead: a closed set, looked up with `getValue`, and
 * covered by a test that iterates the enum. **Adding a concept without writing its copy breaks the
 * build**, which is the only reliable way to keep explanatory text a first-class part of a feature
 * rather than the thing that gets cut last.
 *
 * ## The one-line / body split
 *
 * [PrimerEntry.oneLine] is shown **without tapping** — in a legend, beside a ring, under a value. If
 * clarity only ever arrives on tap, most readers never get it, so the short form has to carry a real
 * idea on its own rather than being a teaser for the long one.
 *
 * ## Voice
 *
 * Matching [PanchangaGlossary]: brief, factual, concrete numbers where they help, third person, no
 * instructions to the reader. Traditional claims are attributed ("traditionally", "is said to")
 * rather than asserted, and the mechanical facts — degrees, counts, durations — are stated plainly,
 * because those are what actually answer a beginner's question.
 */
object PanchangaPrimer {
    /** The explanation of [concept]. Never null; [PanchangaConcept] is covered exhaustively. */
    fun of(concept: PanchangaConcept): PrimerEntry = ENTRIES.getValue(concept)

    private val ENTRIES: Map<PanchangaConcept, PrimerEntry> =
        mapOf(
            PanchangaConcept.PANCHANGA to
                PrimerEntry(
                    title = "Panchanga",
                    oneLine = "Five measures of the day — literally, \"five limbs\".",
                    body =
                        "A panchanga describes a day with five values: the tithi, the vara, the " +
                            "nakshatra, the yoga and the karana. All five come from where the Sun and " +
                            "the Moon actually are, so all five change at their own pace and none of " +
                            "them lines up neatly with the calendar. Reading a day means reading the " +
                            "five together rather than any one alone.",
                ),
            PanchangaConcept.TITHI to
                PrimerEntry(
                    title = "Tithi",
                    oneLine = "The lunar day — how far the Moon has pulled ahead of the Sun.",
                    body =
                        "A tithi is the time the Moon takes to gain another 12° on the Sun, so a " +
                            "lunar month holds thirty of them. The Moon's speed varies, so a tithi " +
                            "runs anywhere from about 19 to 26 hours — which is why it starts and " +
                            "ends at a different clock time each day instead of at midnight. A day is " +
                            "named after whichever tithi is running at sunrise.",
                ),
            PanchangaConcept.PAKSHA to
                PrimerEntry(
                    title = "Paksha",
                    oneLine = "The fortnight — waxing towards full, or waning towards new.",
                    body =
                        "The thirty tithis of a lunar month split into two fortnights of fifteen. " +
                            "Shukla paksha runs from the new moon to the full moon, with the lit part " +
                            "of the Moon growing each night; Krishna paksha runs from full back to " +
                            "new as it shrinks. The same tithi name occurs once in each, so the " +
                            "paksha is what tells them apart.",
                ),
            PanchangaConcept.VARA to
                PrimerEntry(
                    title = "Vara",
                    oneLine = "The weekday — but counted from sunrise, not from midnight.",
                    body =
                        "The seven varas are the familiar weekdays, each named for a graha. The " +
                            "difference is where the day begins: a vara runs sunrise to sunrise, so " +
                            "the hours after midnight still belong to the previous day. This is also " +
                            "why the windows measured from sunrise — Rahu Kalam among them — shift by " +
                            "a few minutes daily and sit differently in summer than in winter.",
                ),
            PanchangaConcept.NAKSHATRA to
                PrimerEntry(
                    title = "Nakshatra",
                    oneLine = "One of 27 equal segments of the Moon's path, each 13°20' wide.",
                    body =
                        "The Moon's circuit is divided into twenty-seven nakshatras rather than the " +
                            "twelve rashis, so it crosses roughly one a day. Each carries a " +
                            "traditional name and character. The nakshatra the Moon occupied at birth " +
                            "is the janma nakshatra, which is what most matching and dasha " +
                            "calculations are built from.",
                ),
            PanchangaConcept.PADA to
                PrimerEntry(
                    title = "Pada",
                    oneLine = "A quarter of a nakshatra — 3°20' of the Moon's path.",
                    body =
                        "Each nakshatra divides into four padas, giving 108 in all. The pada is what " +
                            "connects a nakshatra to a navamsha sign, so it matters for chart work " +
                            "even though it is rarely quoted on its own. The Moon spends roughly six " +
                            "hours in each.",
                ),
            PanchangaConcept.YOGA to
                PrimerEntry(
                    title = "Yoga",
                    oneLine = "A 27-part cycle from the Sun's and Moon's positions added together.",
                    body =
                        "Yoga is found by adding the Sun's and Moon's longitudes and dividing the " +
                            "total into twenty-seven parts — it is not a posture or a practice, " +
                            "despite the shared word. Where the tithi measures the gap between the " +
                            "two bodies, the yoga measures how far the pair has travelled as a whole, " +
                            "so it turns over roughly once a day.",
                ),
            PanchangaConcept.KARANA to
                PrimerEntry(
                    title = "Karana",
                    oneLine = "Half a tithi — the quickest of the five limbs.",
                    body =
                        "Each tithi divides into two karanas, so a lunar month holds sixty of them, " +
                            "each lasting about half a day. They are drawn from eleven names: seven " +
                            "that repeat around the month, and four fixed ones that occur once each. " +
                            "It is the fastest limb to change, so it is the one most likely to differ " +
                            "between morning and evening.",
                ),
            PanchangaConcept.LUNAR_MONTH to
                PrimerEntry(
                    title = "Lunar month",
                    oneLine = "Thirty tithis — about 29.5 days, so it drifts against the calendar.",
                    body =
                        "A lunar month runs from one new moon to the next, roughly 29.5 days, which " +
                            "is about a day short of most calendar months. That shortfall is why " +
                            "festivals fixed to a tithi land on a different date each year, and why " +
                            "the lunar and solar years need an extra month (adhika maasa) every few " +
                            "years to stay in step.",
                ),
            PanchangaConcept.SUNRISE_DAY to
                PrimerEntry(
                    title = "The day begins at sunrise",
                    oneLine = "Traditionally the day starts at sunrise, not at midnight.",
                    body =
                        "Almost everything a panchanga measures is counted from sunrise: the day's " +
                            "name, the eight equal parts that place Rahu Kalam, the fifteen that " +
                            "place Abhijit. Two consequences catch people out. The hours just after " +
                            "midnight still belong to yesterday's vara, and the day's tithi can " +
                            "change at any hour — often in the middle of the afternoon.",
                ),
            PanchangaConcept.MOON_PHASE to
                PrimerEntry(
                    title = "Moon phase",
                    oneLine = "How much of the Moon is lit — a picture of the tithi.",
                    body =
                        "The phase and the tithi measure the same thing from different ends: both " +
                            "follow the angle between the Sun and the Moon. New moon opens Shukla " +
                            "paksha, full moon closes it, and the shape in the sky is a rough check " +
                            "on the number — a half-lit Moon means the eighth tithi of a fortnight, " +
                            "give or take a few hours.",
                ),
        )
}

/**
 * A panchanga idea a reader may want explained, as distinct from a named item like "Rahu Kalam".
 *
 * Closed on purpose: [PanchangaPrimer] covers every entry, and a test over `entries` fails the build
 * if a new concept is added without copy.
 */
enum class PanchangaConcept {
    PANCHANGA,
    TITHI,
    PAKSHA,
    VARA,
    NAKSHATRA,
    PADA,
    YOGA,
    KARANA,
    LUNAR_MONTH,
    SUNRISE_DAY,
    MOON_PHASE,
}

/**
 * One explanation, in two lengths.
 *
 * @property title what a detail sheet is headed with.
 * @property oneLine a single idea, short enough to sit beside a value without tapping. Kept under
 *   [ONE_LINE_MAX_CHARS] so it survives a narrow screen at a large font scale.
 * @property body two to four sentences, for a reader who tapped because they wanted more.
 */
data class PrimerEntry(
    val title: String,
    val oneLine: String,
    val body: String,
) {
    companion object {
        /** The width a one-liner has to live within, checked by `PanchangaPrimerTest`. */
        const val ONE_LINE_MAX_CHARS = 72
    }
}
