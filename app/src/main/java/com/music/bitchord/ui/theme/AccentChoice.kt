package com.music.bitchord.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AccentMode
import com.music.bitchord.data.settings.AppSettings

/** The ready-made colours offered under Settings → Accent colour. */
val AccentPresets: List<Color> = listOf(
    Color(0xFFFF7A59), // coral
    Color(0xFFFF4F7B), // rose
    Color(0xFFF2453D), // red
    Color(0xFFFF9F0A), // orange
    Color(0xFFFFCC00), // amber
    Color(0xFFA3E635), // lime
    Color(0xFF30D158), // green
    Color(0xFF2DD4BF), // teal
    Color(0xFF38BDF8), // sky
    Color(0xFF3B82F6), // blue
    Color(0xFF6366F1), // indigo
    Color(0xFFA855F7), // violet
    Color(0xFFEC4899), // pink
)

/**
 * The phone's own colours, drawn from the wallpaper on Android 12 and later:
 * the three accent families at a tone that reads on a dark page, then the
 * same three a step deeper. Empty before Android 12, which has no palette.
 */
object SystemAccents {
    val available: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun colors(context: Context): List<Color> =
        if (available) read(context) else emptyList()

    @RequiresApi(Build.VERSION_CODES.S)
    private fun read(context: Context): List<Color> = listOf(
        android.R.color.system_accent1_300,
        android.R.color.system_accent2_300,
        android.R.color.system_accent3_300,
        android.R.color.system_accent1_500,
        android.R.color.system_accent2_500,
        android.R.color.system_accent3_500,
    ).map { Color(context.getColor(it)) }
}

/**
 * The accent as chosen in Settings, worked out into colours: the icon's
 * gradient, a system colour (which follows the wallpaper as it changes), or
 * the listener's own colour. A system choice that can no longer be honoured —
 * a restore onto an older phone — falls back to the icon's gradient.
 */
@Composable
fun rememberAccentColors(): AccentColors {
    val context = LocalContext.current
    val mode by AppSettings.accentMode.collectAsStateWithLifecycle()
    val color by AppSettings.accentColor.collectAsStateWithLifecycle()
    val systemIndex by AppSettings.accentSystemIndex.collectAsStateWithLifecycle()
    // The configuration is read so a wallpaper change, which recreates the
    // activity's resources, brings the new system colours with it.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return remember(mode, color, systemIndex, configuration) {
        when (mode) {
            AccentMode.ICON -> AccentColors.Icon
            AccentMode.COLOR -> AccentColors.single(Color(color))
            AccentMode.SYSTEM -> SystemAccents.colors(context).getOrNull(systemIndex)
                ?.let(AccentColors::single)
                ?: AccentColors.Icon
        }
    }
}
