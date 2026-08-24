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

package io.github.vedicmitra.core.astronomy

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The varga rule alone, with the ephemeris taken out of the question.
 *
 * [VargaTest] checks our whole pipeline against an independent implementation's charts, which asks
 * two things at once: does our rule agree with theirs, and do our longitudes agree with theirs. Those
 * fail differently and want separating. A D-45 golden failed in review not because the rule was wrong
 * but because our Saturn sat 4.7 arcminutes from theirs in 1975, which is a whole division at that
 * width — the rule reproduced their sign exactly once fed their own longitude.
 *
 * So this test feeds **their longitudes** to [vargaSign] and asserts **their signs**. No tolerance and
 * nothing to skip: a failure here means the rule is wrong, full stop. The end-to-end comparison stays
 * in [VargaTest], where a band is appropriate because two ephemerides really do disagree.
 *
 * 240 placements drawn from a fit over 8,320, sampled to include, for every varga, the four sitting
 * closest to a division edge — the cases a random sample would under-represent and the ones a wrong
 * table or an off-by-one in the bucketing would break first.
 */
class VargaRuleTest {
    @Test
    fun `the rule reproduces the reference sign from the reference longitude`() {
        val mismatches = mutableListOf<String>()
        RULE_GOLDENS.forEach { golden ->
            val actual = vargaSign(golden.varga, golden.siderealLongitude).name
            if (actual != golden.expected) {
                mismatches +=
                    "${golden.varga.displayName} at ${golden.siderealLongitude}: " +
                    "expected ${golden.expected}, got $actual"
            }
        }
        assertWithMessage(mismatches.joinToString("\n")).that(mismatches).isEmpty()
    }

    @Test
    fun `every varga is covered`() {
        assertWithMessage("a varga with no rule goldens would pass this suite by default")
            .that(RULE_GOLDENS.map { it.varga }.toSet())
            .containsAtLeastElementsIn(Varga.entries.filter { it != Varga.D1 })
    }
}

/** One placement as the reference implementation reports it, keyed to its own longitude. */
private data class RuleGolden(
    val varga: Varga,
    val siderealLongitude: Double,
    val expected: String,
)

private val RULE_GOLDENS =
    listOf(
        RuleGolden(Varga.D3, 290.0174, "Kanya"),
        RuleGolden(Varga.D3, 249.9645, "Dhanu"),
        RuleGolden(Varga.D3, 240.0409, "Dhanu"),
        RuleGolden(Varga.D3, 230.0605, "Karka"),
        RuleGolden(Varga.D3, 209.8666, "Mithuna"),
        RuleGolden(Varga.D3, 209.6895, "Mithuna"),
        RuleGolden(Varga.D3, 263.6046, "Simha"),
        RuleGolden(Varga.D3, 193.8394, "Kumbha"),
        RuleGolden(Varga.D3, 86.2393, "Kumbha"),
        RuleGolden(Varga.D3, 266.7706, "Simha"),
        RuleGolden(Varga.D3, 266.2393, "Simha"),
        RuleGolden(Varga.D3, 151.8162, "Kanya"),
        RuleGolden(Varga.D3, 166.4066, "Makara"),
        RuleGolden(Varga.D3, 267.5324, "Simha"),
        RuleGolden(Varga.D3, 181.1278, "Tula"),
        RuleGolden(Varga.D4, 352.4973, "Kanya"),
        RuleGolden(Varga.D4, 224.9890, "Kumbha"),
        RuleGolden(Varga.D4, 44.9890, "Simha"),
        RuleGolden(Varga.D4, 307.5139, "Vrishabha"),
        RuleGolden(Varga.D4, 156.1133, "Kanya"),
        RuleGolden(Varga.D4, 31.6356, "Vrishabha"),
        RuleGolden(Varga.D4, 214.2968, "Vrishchika"),
        RuleGolden(Varga.D4, 209.8666, "Karka"),
        RuleGolden(Varga.D4, 165.0525, "Meena"),
        RuleGolden(Varga.D4, 62.0112, "Mithuna"),
        RuleGolden(Varga.D4, 99.6049, "Tula"),
        RuleGolden(Varga.D4, 197.9441, "Mesha"),
        RuleGolden(Varga.D4, 168.1835, "Meena"),
        RuleGolden(Varga.D4, 141.5793, "Kumbha"),
        RuleGolden(Varga.D4, 161.7208, "Dhanu"),
        RuleGolden(Varga.D6, 224.9890, "Dhanu"),
        RuleGolden(Varga.D6, 44.9890, "Dhanu"),
        RuleGolden(Varga.D6, 290.0174, "Kumbha"),
        RuleGolden(Varga.D6, 249.9645, "Vrishabha"),
        RuleGolden(Varga.D6, 229.8672, "Makara"),
        RuleGolden(Varga.D6, 266.8208, "Kanya"),
        RuleGolden(Varga.D6, 195.7488, "Karka"),
        RuleGolden(Varga.D6, 207.2712, "Kanya"),
        RuleGolden(Varga.D6, 176.8141, "Meena"),
        RuleGolden(Varga.D6, 202.1555, "Simha"),
        RuleGolden(Varga.D6, 320.4137, "Simha"),
        RuleGolden(Varga.D6, 198.4982, "Karka"),
        RuleGolden(Varga.D6, 266.4710, "Kanya"),
        RuleGolden(Varga.D6, 234.0922, "Kumbha"),
        RuleGolden(Varga.D6, 77.4490, "Karka"),
        RuleGolden(Varga.D7, 201.4295, "Meena"),
        RuleGolden(Varga.D7, 94.2908, "Kumbha"),
        RuleGolden(Varga.D7, 141.4396, "Makara"),
        RuleGolden(Varga.D7, 214.2968, "Mithuna"),
        RuleGolden(Varga.D7, 190.0728, "Dhanu"),
        RuleGolden(Varga.D7, 14.3346, "Karka"),
        RuleGolden(Varga.D7, 124.9464, "Kanya"),
        RuleGolden(Varga.D7, 197.5705, "Kumbha"),
        RuleGolden(Varga.D7, 254.3072, "Meena"),
        RuleGolden(Varga.D7, 338.2004, "Tula"),
        RuleGolden(Varga.D7, 238.6397, "Vrishchika"),
        RuleGolden(Varga.D7, 285.5162, "Tula"),
        RuleGolden(Varga.D7, 57.9349, "Vrishabha"),
        RuleGolden(Varga.D7, 248.3692, "Makara"),
        RuleGolden(Varga.D7, 253.4814, "Meena"),
        RuleGolden(Varga.D8, 352.4973, "Makara"),
        RuleGolden(Varga.D8, 266.2393, "Kumbha"),
        RuleGolden(Varga.D8, 86.2393, "Kumbha"),
        RuleGolden(Varga.D8, 224.9890, "Meena"),
        RuleGolden(Varga.D8, 138.7034, "Mesha"),
        RuleGolden(Varga.D8, 304.0869, "Makara"),
        RuleGolden(Varga.D8, 107.6540, "Simha"),
        RuleGolden(Varga.D8, 123.3659, "Dhanu"),
        RuleGolden(Varga.D8, 359.0629, "Meena"),
        RuleGolden(Varga.D8, 172.3097, "Makara"),
        RuleGolden(Varga.D8, 64.7600, "Kanya"),
        RuleGolden(Varga.D8, 246.4015, "Kanya"),
        RuleGolden(Varga.D8, 281.0891, "Mithuna"),
        RuleGolden(Varga.D8, 226.1570, "Mesha"),
        RuleGolden(Varga.D8, 256.5150, "Dhanu"),
        RuleGolden(Varga.D9, 216.6796, "Kanya"),
        RuleGolden(Varga.D9, 36.6796, "Meena"),
        RuleGolden(Varga.D9, 290.0174, "Karka"),
        RuleGolden(Varga.D9, 176.6869, "Kanya"),
        RuleGolden(Varga.D9, 214.2968, "Simha"),
        RuleGolden(Varga.D9, 214.3645, "Simha"),
        RuleGolden(Varga.D9, 355.4171, "Kumbha"),
        RuleGolden(Varga.D9, 76.3264, "Kumbha"),
        RuleGolden(Varga.D9, 283.4670, "Vrishabha"),
        RuleGolden(Varga.D9, 147.3009, "Dhanu"),
        RuleGolden(Varga.D9, 234.0922, "Kumbha"),
        RuleGolden(Varga.D9, 347.1607, "Dhanu"),
        RuleGolden(Varga.D9, 17.4650, "Kanya"),
        RuleGolden(Varga.D9, 81.5960, "Mesha"),
        RuleGolden(Varga.D9, 81.7327, "Mesha"),
        RuleGolden(Varga.D10, 227.9990, "Dhanu"),
        RuleGolden(Varga.D10, 312.0045, "Mithuna"),
        RuleGolden(Varga.D10, 188.9911, "Dhanu"),
        RuleGolden(Varga.D10, 224.9890, "Vrishchika"),
        RuleGolden(Varga.D10, 116.4800, "Vrishchika"),
        RuleGolden(Varga.D10, 305.8331, "Meena"),
        RuleGolden(Varga.D10, 36.6796, "Meena"),
        RuleGolden(Varga.D10, 294.1810, "Vrishabha"),
        RuleGolden(Varga.D10, 165.7213, "Tula"),
        RuleGolden(Varga.D10, 231.7273, "Kumbha"),
        RuleGolden(Varga.D10, 209.6895, "Karka"),
        RuleGolden(Varga.D10, 208.4133, "Karka"),
        RuleGolden(Varga.D10, 79.2161, "Dhanu"),
        RuleGolden(Varga.D10, 193.8394, "Kumbha"),
        RuleGolden(Varga.D10, 290.0174, "Meena"),
        RuleGolden(Varga.D11, 207.2712, "Karka"),
        RuleGolden(Varga.D11, 231.8263, "Vrishabha"),
        RuleGolden(Varga.D11, 204.5541, "Karka"),
        RuleGolden(Varga.D11, 130.9180, "Mesha"),
        RuleGolden(Varga.D11, 193.8394, "Meena"),
        RuleGolden(Varga.D11, 321.5912, "Makara"),
        RuleGolden(Varga.D11, 214.3645, "Tula"),
        RuleGolden(Varga.D11, 14.4004, "Kanya"),
        RuleGolden(Varga.D11, 9.1670, "Karka"),
        RuleGolden(Varga.D11, 225.0450, "Kumbha"),
        RuleGolden(Varga.D11, 240.0409, "Simha"),
        RuleGolden(Varga.D11, 246.4015, "Tula"),
        RuleGolden(Varga.D11, 172.3097, "Karka"),
        RuleGolden(Varga.D11, 175.3305, "Simha"),
        RuleGolden(Varga.D11, 267.5324, "Mithuna"),
        RuleGolden(Varga.D12, 352.4973, "Vrishchika"),
        RuleGolden(Varga.D12, 224.9890, "Mesha"),
        RuleGolden(Varga.D12, 44.9890, "Tula"),
        RuleGolden(Varga.D12, 307.5139, "Vrishabha"),
        RuleGolden(Varga.D12, 35.0640, "Karka"),
        RuleGolden(Varga.D12, 246.4015, "Kumbha"),
        RuleGolden(Varga.D12, 243.2741, "Makara"),
        RuleGolden(Varga.D12, 225.0450, "Vrishabha"),
        RuleGolden(Varga.D12, 325.5987, "Dhanu"),
        RuleGolden(Varga.D12, 99.7846, "Tula"),
        RuleGolden(Varga.D12, 115.9733, "Vrishabha"),
        RuleGolden(Varga.D12, 185.4378, "Dhanu"),
        RuleGolden(Varga.D12, 228.3402, "Mithuna"),
        RuleGolden(Varga.D12, 213.9969, "Dhanu"),
        RuleGolden(Varga.D12, 226.9870, "Vrishabha"),
        RuleGolden(Varga.D16, 352.4973, "Vrishchika"),
        RuleGolden(Varga.D16, 155.6202, "Kumbha"),
        RuleGolden(Varga.D16, 335.6202, "Kumbha"),
        RuleGolden(Varga.D16, 208.1182, "Mithuna"),
        RuleGolden(Varga.D16, 131.3877, "Kumbha"),
        RuleGolden(Varga.D16, 357.0327, "Kumbha"),
        RuleGolden(Varga.D16, 107.6540, "Makara"),
        RuleGolden(Varga.D16, 194.1845, "Vrishchika"),
        RuleGolden(Varga.D16, 230.0605, "Mithuna"),
        RuleGolden(Varga.D16, 107.5659, "Makara"),
        RuleGolden(Varga.D16, 66.4015, "Meena"),
        RuleGolden(Varga.D16, 146.4998, "Tula"),
        RuleGolden(Varga.D16, 81.5960, "Vrishchika"),
        RuleGolden(Varga.D16, 200.5744, "Kumbha"),
        RuleGolden(Varga.D16, 256.1945, "Simha"),
        RuleGolden(Varga.D20, 227.9990, "Vrishchika"),
        RuleGolden(Varga.D20, 352.4973, "Tula"),
        RuleGolden(Varga.D20, 312.0045, "Simha"),
        RuleGolden(Varga.D20, 238.4934, "Mithuna"),
        RuleGolden(Varga.D20, 148.9435, "Karka"),
        RuleGolden(Varga.D20, 303.8441, "Kumbha"),
        RuleGolden(Varga.D20, 191.2795, "Vrishchika"),
        RuleGolden(Varga.D20, 305.8331, "Meena"),
        RuleGolden(Varga.D20, 107.6540, "Meena"),
        RuleGolden(Varga.D20, 86.2393, "Makara"),
        RuleGolden(Varga.D20, 320.1138, "Makara"),
        RuleGolden(Varga.D20, 242.9307, "Kanya"),
        RuleGolden(Varga.D20, 223.6246, "Kanya"),
        RuleGolden(Varga.D20, 245.0818, "Vrishchika"),
        RuleGolden(Varga.D20, 18.6612, "Mesha"),
        RuleGolden(Varga.D24, 276.2477, "Vrishchika"),
        RuleGolden(Varga.D24, 352.4973, "Dhanu"),
        RuleGolden(Varga.D24, 193.7583, "Karka"),
        RuleGolden(Varga.D24, 266.2393, "Mesha"),
        RuleGolden(Varga.D24, 165.0525, "Karka"),
        RuleGolden(Varga.D24, 76.3264, "Kanya"),
        RuleGolden(Varga.D24, 260.6358, "Dhanu"),
        RuleGolden(Varga.D24, 73.6136, "Mithuna"),
        RuleGolden(Varga.D24, 95.0370, "Vrishchika"),
        RuleGolden(Varga.D24, 174.9520, "Kumbha"),
        RuleGolden(Varga.D24, 15.2141, "Simha"),
        RuleGolden(Varga.D24, 290.1068, "Vrishchika"),
        RuleGolden(Varga.D24, 145.6952, "Mesha"),
        RuleGolden(Varga.D24, 320.9400, "Dhanu"),
        RuleGolden(Varga.D24, 18.1991, "Tula"),
        RuleGolden(Varga.D27, 37.7771, "Makara"),
        RuleGolden(Varga.D27, 72.2245, "Kanya"),
        RuleGolden(Varga.D27, 287.7718, "Tula"),
        RuleGolden(Varga.D27, 184.4554, "Kumbha"),
        RuleGolden(Varga.D27, 247.7568, "Tula"),
        RuleGolden(Varga.D27, 210.4808, "Makara"),
        RuleGolden(Varga.D27, 352.3697, "Kanya"),
        RuleGolden(Varga.D27, 139.0495, "Kanya"),
        RuleGolden(Varga.D27, 212.0925, "Kumbha"),
        RuleGolden(Varga.D27, 304.0869, "Makara"),
        RuleGolden(Varga.D27, 238.4934, "Kumbha"),
        RuleGolden(Varga.D27, 61.6848, "Vrishchika"),
        RuleGolden(Varga.D27, 180.8313, "Tula"),
        RuleGolden(Varga.D27, 356.4808, "Dhanu"),
        RuleGolden(Varga.D27, 19.0390, "Kanya"),
        RuleGolden(Varga.D40, 227.9990, "Kanya"),
        RuleGolden(Varga.D40, 195.7488, "Dhanu"),
        RuleGolden(Varga.D40, 352.4973, "Meena"),
        RuleGolden(Varga.D40, 312.0045, "Simha"),
        RuleGolden(Varga.D40, 230.0605, "Dhanu"),
        RuleGolden(Varga.D40, 252.3196, "Simha"),
        RuleGolden(Varga.D40, 245.5293, "Vrishchika"),
        RuleGolden(Varga.D40, 326.5047, "Meena"),
        RuleGolden(Varga.D40, 12.0865, "Simha"),
        RuleGolden(Varga.D40, 57.9349, "Vrishchika"),
        RuleGolden(Varga.D40, 283.4670, "Meena"),
        RuleGolden(Varga.D40, 87.8670, "Vrishabha"),
        RuleGolden(Varga.D40, 166.4066, "Karka"),
        RuleGolden(Varga.D40, 226.5128, "Simha"),
        RuleGolden(Varga.D40, 197.9441, "Meena"),
        RuleGolden(Varga.D45, 270.6657, "Mesha"),
        RuleGolden(Varga.D45, 227.9990, "Tula"),
        RuleGolden(Varga.D45, 344.6656, "Kanya"),
        RuleGolden(Varga.D45, 212.6678, "Dhanu"),
        RuleGolden(Varga.D45, 195.2141, "Kumbha"),
        RuleGolden(Varga.D45, 236.1067, "Vrishchika"),
        RuleGolden(Varga.D45, 328.4263, "Kumbha"),
        RuleGolden(Varga.D45, 313.0102, "Meena"),
        RuleGolden(Varga.D45, 132.7283, "Meena"),
        RuleGolden(Varga.D45, 129.3981, "Tula"),
        RuleGolden(Varga.D45, 307.5139, "Karka"),
        RuleGolden(Varga.D45, 118.4066, "Tula"),
        RuleGolden(Varga.D45, 58.1451, "Kumbha"),
        RuleGolden(Varga.D45, 131.3877, "Makara"),
        RuleGolden(Varga.D45, 238.2886, "Kumbha"),
        RuleGolden(Varga.D60, 146.4998, "Dhanu"),
        RuleGolden(Varga.D60, 227.9990, "Tula"),
        RuleGolden(Varga.D60, 18.4982, "Mesha"),
        RuleGolden(Varga.D60, 198.4982, "Tula"),
        RuleGolden(Varga.D60, 25.1269, "Mithuna"),
        RuleGolden(Varga.D60, 359.0629, "Makara"),
        RuleGolden(Varga.D60, 301.6561, "Vrishabha"),
        RuleGolden(Varga.D60, 40.7254, "Kumbha"),
        RuleGolden(Varga.D60, 167.1607, "Karka"),
        RuleGolden(Varga.D60, 205.1269, "Dhanu"),
        RuleGolden(Varga.D60, 215.0640, "Kanya"),
        RuleGolden(Varga.D60, 168.1885, "Kanya"),
        RuleGolden(Varga.D60, 62.8344, "Vrishchika"),
        RuleGolden(Varga.D60, 123.3659, "Kumbha"),
        RuleGolden(Varga.D60, 347.1607, "Makara"),
    )
