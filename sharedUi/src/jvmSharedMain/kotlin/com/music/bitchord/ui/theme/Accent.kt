package com.music.bitchord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** An accent as two ends of a run: one colour for anything flat, both for a gradient. */
@Immutable
data class AccentColors(val start: Color, val end: Color) {
    companion object {
        /** The app icon's coral running to purple. */
        val Icon = AccentColors(Color(0xFFFF7A59), Color(0xFF7B2FF7))

        /**
         * One chosen colour, run into a deeper, slightly turned version of
         * itself so a gradient still has somewhere to go.
         */
        fun single(color: Color): AccentColors {
            val (h, s, v) = color.toHsv()
            return AccentColors(color, Color.hsv((h + 28f) % 360f, s, (v * 0.78f).coerceIn(0f, 1f)))
        }
    }
}

/**
 * TuneTable's accent, as chosen in Settings → Appearance → Accent colour.
 *
 * Held as Compose state, so everything drawn in it — a lit glyph, a switch,
 * the selected tab — follows a new choice at once. [Solid] is the colour for
 * anything drawn flat; [brush] and [sweep] are the full run, for the few marks
 * big enough to show a gradient.
 */
object Accent {
    var colors: AccentColors by mutableStateOf(AccentColors.Icon)

    val Start: Color get() = colors.start
    val End: Color get() = colors.end
    val Solid: Color get() = colors.start

    /** Text or a glyph drawn on [Solid]: black on a light accent, white on a dark one. */
    val OnSolid: Color get() = if (Solid.luminance() > 0.55f) Color.Black else Color.White

    /** The run, corner to corner across whatever it fills. */
    val brush: Brush get() = Brush.linearGradient(listOf(Start, End))

    /** The same run as an angle sweep, for a ring. */
    val sweep: Brush get() = Brush.sweepGradient(listOf(Start, End, Start))
}

/** Hue (0–360), saturation and value (0–1) of an opaque colour. */
fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (max == 0f) 0f else delta / max
    return Triple(hue, saturation, max)
}
