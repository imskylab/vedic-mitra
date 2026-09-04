/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.profile

import androidx.annotation.StringRes
import io.github.vedicmitra.core.datastore.Gender
import io.github.vedicmitra.core.datastore.ProfileRelation

/**
 * What [ProfileRelation] and [Gender] are called on screen.
 *
 * The enums used to carry a `displayName`, which put English copy in `:core:datastore` — a module
 * that persists data and knows nothing about a locale. That is the same mistake as keying a reminder
 * on a label (ADR 0019): a value the data layer owns and the UI layer names had one field doing both
 * jobs, and the display half cannot be translated where it sat.
 *
 * These enums are persisted **by enum name**, so the labels here are free to change without
 * migrating anything — which is exactly the separation that was missing.
 */
@get:StringRes
internal val ProfileRelation.labelRes: Int
    get() =
        when (this) {
            ProfileRelation.SELF -> R.string.profile_relation_self
            ProfileRelation.SPOUSE -> R.string.profile_relation_spouse
            ProfileRelation.CHILD -> R.string.profile_relation_child
            ProfileRelation.PARENT -> R.string.profile_relation_parent
            ProfileRelation.FRIEND -> R.string.profile_relation_friend
            ProfileRelation.OTHER -> R.string.profile_relation_other
        }

@get:StringRes
internal val Gender.labelRes: Int
    get() =
        when (this) {
            Gender.MALE -> R.string.profile_gender_male
            Gender.FEMALE -> R.string.profile_gender_female
            Gender.OTHER -> R.string.profile_gender_other
        }
