/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.core.datastore

import com.google.common.truth.Truth.assertThat
import io.github.vedicmitra.core.common.model.GeoCoordinates
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@Suppress("MagicNumber")
class ProfileCodecTest {
    private val separator = Char(0x1F).toString()

    private val full =
        BirthProfile(
            id = "a",
            name = "Leo",
            relation = ProfileRelation.SELF,
            gender = Gender.MALE,
            dateOfBirth = LocalDate.of(1995, 3, 14),
            timeOfBirth = LocalTime.of(9, 30),
            placeOfBirth = "Hyderabad, India",
            birthCoordinates = GeoCoordinates(latitude = 17.385, longitude = 78.4867),
            birthZoneId = "Asia/Kolkata",
        )

    @Test
    fun `round-trips a profile including its gender`() {
        assertThat(ProfileCodec.decode(ProfileCodec.encode(full))).isEqualTo(full)
    }

    @Test
    fun `round-trips a profile with no gender set`() {
        val noGender = full.copy(gender = null)
        assertThat(ProfileCodec.decode(ProfileCodec.encode(noGender))).isEqualTo(noGender)
    }

    @Test
    fun `decodes the legacy layout that predates gender, defaulting gender to null`() {
        // The original nine-field order: id, relation, date, time, lat, lng, zone, name, place.
        val legacy =
            listOf(
                "a",
                ProfileRelation.SELF.name,
                "1995-03-14",
                "09:30",
                "17.385",
                "78.4867",
                "Asia/Kolkata",
                "Leo",
                "Hyderabad, India",
            ).joinToString(separator)

        assertThat(ProfileCodec.decode(legacy)).isEqualTo(full.copy(gender = null))
    }

    @Test
    fun `returns null for a malformed value`() {
        assertThat(ProfileCodec.decode("not a profile")).isNull()
    }
}
