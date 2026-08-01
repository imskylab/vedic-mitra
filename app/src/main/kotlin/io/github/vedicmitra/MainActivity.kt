/*
 * Copyright (c) 2026 Vedic Mitra contributors
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.vedicmitra.core.designsystem.theme.VedicMitraTheme

/**
 * Single-activity host for the app. Sets up edge-to-edge Compose content and the app theme.
 *
 * The navigation graph and top-level app scaffolding are added in later phases; for now this hosts
 * a placeholder so the foundation compiles and launches. Annotated with [AndroidEntryPoint] so
 * Hilt can inject into it once dependencies exist.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VedicMitraTheme {
                VedicMitraAppPlaceholder()
            }
        }
    }
}

/** Temporary placeholder shown until the navigation host lands in a later phase. */
@Composable
private fun VedicMitraAppPlaceholder() {
    Scaffold { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Vedic Mitra")
        }
    }
}
