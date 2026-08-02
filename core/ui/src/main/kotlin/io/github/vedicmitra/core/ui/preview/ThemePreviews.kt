/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for full license text.
 */

package io.github.vedicmitra.core.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation that renders a composable in both light and dark themes at once. Annotate
 * `@Composable` preview functions with `@ThemePreviews` to review both variants side by side.
 *
 * Foundation UI tooling — used by previews across features.
 */
@Preview(name = "Light", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
annotation class ThemePreviews
