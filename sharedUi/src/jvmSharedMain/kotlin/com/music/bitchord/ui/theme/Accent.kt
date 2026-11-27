package com.music.bitchord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor

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
 * How strongly the accent colours the app — Settings → Appearance → Style.
 */
enum class AccentStyle {
    /** A hint of the accent in the backgrounds; the accent itself for highlights. */
    SUBTLE,

    /** The accent soaks every surface; fills run the full gradient. */
    VIBRANT,

    /** A darker, richer version of the accent everywhere. */
    DEEP,
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

    /** The style in force — see [AccentStyle]. */
    var style: AccentStyle by mutableStateOf(AccentStyle.SUBTLE)

    /**
     * How much stronger the accent washes over cards, menus and drawers run in
     * this style. Deep's surfaces are already the accent's own dark colour, so
     * it keeps the plain amount.
     */
    val washScale: Float get() = if (style == AccentStyle.VIBRANT) 2.2f else 1f

    /** The fill of a selected chip, segment or switch, as one colour. */
    val Fill: Color get() = if (style == AccentStyle.DEEP) Start.withLightness(DEEP_FILL_LIGHTNESS) else Start

    /** [Fill] as a brush: the full gradient in Vibrant. */
    val fillBrush: Brush get() = when (style) {
        AccentStyle.VIBRANT -> brush
        else -> SolidColor(Fill)
    }

    /** Text or a glyph drawn on [Fill]. */
    val OnFill: Color get() = if (style == AccentStyle.DEEP || Fill.luminance() <= 0.55f) Color.White else Color.Black

    /**
     * A plain icon drawn in the accent on a surface: lifted towards white in
     * Vibrant, so it still stands out on surfaces the accent already colours.
     */
    val Icon: Color get() = if (style == AccentStyle.VIBRANT) lerp(Start, Color.White, 0.35f) else Start

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

/** Deep's fills: the accent's hue at this lightness. */
private const val DEEP_FILL_LIGHTNESS = 0.42f

/** This colour's hue and saturation at another lightness (0–1), as HSL has it. */
fun Color.withLightness(lightness: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val (hue, _, _) = toHsv()
    val l = (max + min) / 2f
    val d = max - min
    val saturation = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    return Color.hsl(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
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
