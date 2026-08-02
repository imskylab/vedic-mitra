/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
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
