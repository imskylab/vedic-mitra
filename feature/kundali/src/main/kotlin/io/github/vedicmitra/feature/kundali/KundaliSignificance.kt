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

import io.github.vedicmitra.core.astronomy.Graha

/**
 * Plain-language significance for the placements shown on the kundali screen — the ascendant sign, the
 * Moon's nakshatra, the running mahadasha, and each graha's sign/house. These are general classical
 * descriptions of the placement itself (not a personalised reading), shown when a card or row is
 * tapped.
 */
internal object KundaliSignificance {
    /** The ascendant (Lagna) sign's temperament; [rasiIndex] is 0 = Mesha .. 11 = Meena. */
    fun lagna(rasiIndex: Int): String = LAGNA.getOrElse(rasiIndex) { "" }

    /** The Moon's nakshatra nature; [nakshatraNumber] is 1..27. */
    fun moon(nakshatraNumber: Int): String = NAKSHATRA.getOrElse(nakshatraNumber - 1) { "" }

    /** What a mahadasha ruled by [graha] brings. */
    fun dasha(graha: Graha): String =
        when (graha) {
            Graha.SUN ->
                "A Sun (Surya) mahadasha brings the self, authority, vitality, recognition and the " +
                    "father to the fore. It is a time to step into leadership and assert your purpose, with " +
                    "attention to ego and health."
            Graha.MOON ->
                "A Moon (Chandra) mahadasha brings emotions, the mind, home, the mother and public " +
                    "life forward. Life flows with feeling and change; nurturing yourself and others is the keynote."
            Graha.MANGALA ->
                "A Mars (Mangala) mahadasha brings energy, courage, ambition, property and drive. " +
                    "It favours bold action and hard work, with care needed around anger, haste and conflict."
            Graha.BUDHA ->
                "A Mercury (Budha) mahadasha brings intellect, communication, learning, trade and " +
                    "skill. It is excellent for study, business and networking, rewarding a clear, adaptable mind."
            Graha.GURU ->
                "A Jupiter (Guru) mahadasha brings wisdom, growth, fortune, teachers, children and " +
                    "dharma — often one of the most benevolent periods, expansive and spiritually fruitful."
            Graha.SHUKRA ->
                "A Venus (Shukra) mahadasha brings love, relationships, comfort, art, beauty and " +
                    "prosperity. It favours partnership and creativity, asking for balance and moderation."
            Graha.SHANI ->
                "A Saturn (Shani) mahadasha brings discipline, responsibility, hard lessons and " +
                    "slow, lasting rewards. Through patience and honest effort it builds durable maturity."
            Graha.RAHU ->
                "A Rahu mahadasha brings ambition, worldly desire, sudden turns and unconventional " +
                    "paths. It can raise you quickly into new territory; discernment keeps its cravings in check."
            Graha.KETU ->
                "A Ketu mahadasha brings detachment, introspection, endings and spiritual seeking. " +
                    "Outer life may loosen while inner life deepens — a period of letting go."
        }

    /** What [graha] signifies in general. */
    fun graha(graha: Graha): String =
        when (graha) {
            Graha.SUN ->
                "The Sun (Surya) signifies the soul, self, vitality, authority, the father and one's " +
                    "sense of purpose and confidence."
            Graha.MOON ->
                "The Moon (Chandra) signifies the mind, emotions, mother, comfort and the flow of " +
                    "daily feeling."
            Graha.MANGALA ->
                "Mars (Mangala) signifies energy, courage, drive, discipline, siblings, property " +
                    "and the capacity for action."
            Graha.BUDHA ->
                "Mercury (Budha) signifies intellect, speech, communication, commerce, skill and the " +
                    "analytical, adaptable mind."
            Graha.GURU ->
                "Jupiter (Guru) signifies wisdom, expansion, fortune, faith, teachers, children and " +
                    "moral and spiritual growth."
            Graha.SHUKRA ->
                "Venus (Shukra) signifies love, relationships, beauty, art, comfort, refinement and " +
                    "the pleasures of life."
            Graha.SHANI ->
                "Saturn (Shani) signifies discipline, duty, patience, limitation, longevity and " +
                    "lessons learned through time and effort."
            Graha.RAHU ->
                "Rahu, the north lunar node, signifies worldly desire, ambition, obsession, foreign " +
                    "and unconventional matters, and sudden change."
            Graha.KETU ->
                "Ketu, the south lunar node, signifies detachment, spirituality, innate talent, loss " +
                    "and the path of liberation."
        }

    /** The theme of the [house] (1..12). */
    fun house(house: Int): String = HOUSES.getOrElse(house - 1) { "" }

    /** A composed reading for a graha in a sign and house, with a note if retrograde. */
    fun grahaInHouse(
        graha: Graha,
        rasiName: String,
        house: Int,
        retrograde: Boolean,
    ): String =
        buildString {
            append(graha(graha))
            append("\n\n")
            append(house(house))
            append("\n\nHere it sits in ")
            append(rasiName)
            append(", colouring these themes with that sign's temperament.")
            if (retrograde) {
                append(" Retrograde, its energy turns inward and its lessons are revisited more deeply.")
            }
        }

    private val HOUSES =
        listOf(
            "The 1st house (Lagna) governs the self, body, personality, vitality and how you meet the world.",
            "The 2nd house governs wealth, speech, family, food and the values you hold.",
            "The 3rd house governs courage, effort, siblings, communication and short journeys.",
            "The 4th house governs home, mother, comfort, land, vehicles and inner contentment.",
            "The 5th house governs intelligence, creativity, children, romance and past-life merit.",
            "The 6th house governs health, service, daily work, debts, obstacles and rivals.",
            "The 7th house governs marriage, partnership, business relations and the spouse.",
            "The 8th house governs transformation, longevity, hidden things, inheritance and sudden events.",
            "The 9th house governs fortune, dharma, higher wisdom, the guru, the father and long journeys.",
            "The 10th house governs career, status, action in the world, authority and public reputation.",
            "The 11th house governs gains, income, friendships, hopes and the fulfilment of desires.",
            "The 12th house governs loss, expenditure, foreign lands, seclusion, rest and liberation.",
        )

    private val LAGNA =
        listOf(
            "A Mesha (Aries) ascendant, ruled by Mars, gives a bold, pioneering nature — energetic, direct and " +
                "quick to act. You lead from the front and thrive on challenge; patience and follow-through are " +
                "the lessons to cultivate.",
            "A Vrishabha (Taurus) ascendant, ruled by Venus, gives steadiness, patience and a love of comfort, " +
                "beauty and the good things of life. You build slowly and surely; flexibility and letting go are " +
                "the growth.",
            "A Mithuna (Gemini) ascendant, ruled by Mercury, gives a curious, communicative and versatile mind " +
                "— quick-witted, sociable and adaptable. Focus and depth turn your many interests into mastery.",
            "A Karka (Cancer) ascendant, ruled by the Moon, gives a sensitive, nurturing and emotionally attuned " +
                "nature, deeply tied to home and family. Your care is a gift; guarding against moodiness is the work.",
            "A Simha (Leo) ascendant, ruled by the Sun, gives dignity, warmth and natural leadership — generous, " +
                "proud and drawn to recognition. You shine when you lead with the heart rather than the ego.",
            "A Kanya (Virgo) ascendant, ruled by Mercury, gives a precise, analytical and service-minded nature, " +
                "attentive to detail and health. Your discernment is valuable; easing self-criticism brings peace.",
            "A Tula (Libra) ascendant, ruled by Venus, gives charm, diplomacy and a strong sense of fairness, " +
                "harmony and partnership. You weigh every side; decisiveness is the quality to grow.",
            "A Vrishchika (Scorpio) ascendant, ruled by Mars, gives intensity, depth and a penetrating, secretive " +
                "nature with great willpower. Transformation is your theme; trust and letting go are the lessons.",
            "A Dhanu (Sagittarius) ascendant, ruled by Jupiter, gives an optimistic, philosophical and " +
                "freedom-loving nature, drawn to truth, travel and teaching. Grounding the vision in detail " +
                "completes it.",
            "A Makara (Capricorn) ascendant, ruled by Saturn, gives discipline, ambition and a patient, " +
                "responsible nature built for the long climb. You achieve through perseverance; warmth and rest " +
                "balance the drive.",
            "A Kumbha (Aquarius) ascendant, ruled by Saturn, gives an independent, humanitarian and original " +
                "mind, drawn to ideas, groups and reform. Your uniqueness is a strength; emotional closeness is " +
                "the growth.",
            "A Meena (Pisces) ascendant, ruled by Jupiter, gives a compassionate, imaginative and spiritual " +
                "nature, sensitive and easily moved by others. Your empathy is a gift; healthy boundaries keep " +
                "it sustainable.",
        )

    private val NAKSHATRA =
        listOf(
            "Moon in Ashwini (Ketu; the Ashwini Kumaras) gives a quick, pioneering and healing nature — " +
                "energetic, independent and eager to begin. You act on instinct and love a fresh start.",
            "Moon in Bharani (Venus; Yama) gives strong desires, endurance and creative, transformative power. " +
                "You carry burdens with determination and feel life intensely.",
            "Moon in Krittika (Sun; Agni) gives a sharp, fiery and purifying nature — determined, honest and " +
                "cutting through pretence, with a strong appetite for achievement.",
            "Moon in Rohini (Moon; Brahma) gives charm, beauty and a magnetic, creative nature drawn to comfort " +
                "and the senses. You are nurturing and much loved, though attachment can test you.",
            "Moon in Mrigashira (Mars; Soma) gives a curious, gentle and searching nature, ever seeking " +
                "something new. You are restless, communicative and drawn to exploration.",
            "Moon in Ardra (Rahu; Rudra) gives a keen, stormy and probing mind that thrives on change. Emotions " +
                "run deep, and after the storm comes clarity and growth.",
            "Moon in Punarvasu (Jupiter; Aditi) gives an optimistic, wise and resilient nature that renews " +
                "itself after every setback. You are generous, philosophical and home-loving.",
            "Moon in Pushya (Saturn; Brihaspati) gives a nourishing, dutiful and spiritually inclined nature — " +
                "the most auspicious of stars for care and steadiness. You support others selflessly.",
            "Moon in Ashlesha (Mercury; the Nagas) gives an intuitive, penetrating and hypnotic nature with deep " +
                "insight into people. Wisdom comes from mastering intense emotions.",
            "Moon in Magha (Ketu; the Pitris) gives a regal, proud and tradition-honouring nature, respectful of " +
                "lineage and eager for a place of honour. You carry ancestral strength.",
            "Moon in Purva Phalguni (Venus; Bhaga) gives a warm, creative and pleasure-loving nature that enjoys " +
                "leisure, love and generosity. You bring ease and celebration to life.",
            "Moon in Uttara Phalguni (Sun; Aryaman) gives a helpful, reliable and friendly nature that values " +
                "commitment and service. You are generous and steady in partnership.",
            "Moon in Hasta (Moon; Savitr) gives skilful hands, cleverness and a resourceful, hard-working " +
                "nature. You accomplish much through craft and attention to detail.",
            "Moon in Chitra (Mars; Tvashtar) gives an artistic, brilliant and charismatic nature with an eye for " +
                "beauty and design. You create striking things and shine in company.",
            "Moon in Swati (Rahu; Vayu) gives an independent, adaptable and diplomatic nature that values " +
                "freedom and balance. Like the wind, you move flexibly and self-reliantly.",
            "Moon in Vishakha (Jupiter; Indra-Agni) gives a determined, goal-focused and ambitious nature that " +
                "pursues its aims relentlessly. Patience turns your drive into lasting success.",
            "Moon in Anuradha (Saturn; Mitra) gives a devoted, friendly and cooperative nature skilled at " +
                "friendship and teamwork. You succeed through loyalty and honouring bonds.",
            "Moon in Jyeshtha (Mercury; Indra) gives a capable, protective and authoritative nature that rises " +
                "to responsibility. Depth and seniority are yours, along with a need to guard against isolation.",
            "Moon in Mula (Ketu; Nirriti) gives a searching, root-seeking nature that digs to the truth and lets " +
                "go of the inessential. Endings clear the way for spiritual growth.",
            "Moon in Purva Ashadha (Venus; the waters) gives an invincible, persuasive and optimistic nature " +
                "that inspires others. You have conviction and the power to sway.",
            "Moon in Uttara Ashadha (Sun; the Vishwadevas) gives a principled, persevering and honourable nature " +
                "that earns lasting victory through integrity. You lead by example.",
            "Moon in Shravana (Moon; Vishnu) gives a listening, learning and connecting nature, wise through " +
                "hearing and study. You are a keeper and teacher of knowledge.",
            "Moon in Dhanishta (Mars; the Vasus) gives a rhythmic, prosperous and ambitious nature drawn to " +
                "music, wealth and achievement. You are generous and socially adept.",
            "Moon in Shatabhisha (Rahu; Varuna) gives a secretive, healing and independent nature drawn to " +
                "mysteries and remedies. You mend what is broken and value solitude.",
            "Moon in Purva Bhadrapada (Jupiter; Aja Ekapada) gives an intense, idealistic and unconventional " +
                "nature with a spiritual, sometimes fiery edge. You are driven by a higher purpose.",
            "Moon in Uttara Bhadrapada (Saturn; Ahir Budhnya) gives a calm, deep and wise nature — patient, " +
                "compassionate and spiritually grounded. You bring stillness and counsel.",
            "Moon in Revati (Mercury; Pushan) gives a gentle, nurturing and protective nature that guides and " +
                "shelters others. You are kind, imaginative and a safe harbour on the journey.",
        )
}
