/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point and Hilt dependency-injection root.
 *
 * Annotating with [HiltAndroidApp] generates the application-level component that hosts singletons
 * for the whole app. Feature and core modules contribute their bindings through their own Hilt
 * modules; this class deliberately holds no business logic.
 */
@HiltAndroidApp
class VedicMitraApplication : Application()
