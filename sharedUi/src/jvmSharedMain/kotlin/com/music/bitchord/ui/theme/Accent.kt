package com.music.bitchord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
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
 * How the accent colours the app — Settings → Appearance → Style.
 */
enum class AccentStyle {
    /** A hint of the accent in the backgrounds; the accent itself for highlights. */
    SUBTLE,

    /** Surfaces built from the accent's darkest shades, pale accent icons. */
    DEEP,

    /** A dark page under a soft glow of the accent; gradient fills, white icons. */
    GLOW,

    /** Surfaces in the accent's second colour, highlights in its first. */
    DUOTONE,

    /** A near-black page; highlights drawn as thin accent outlines. */
    NEON,
}

/**
 * TuneTable's accent, as chosen in Settings → Appearance → Accent colour.
 *
 * Held as Compose state, so everything drawn in it — a lit glyph, a switch,
 * the selected tab — follows a new choice at once. [Solid] is the colour for
 * anything drawn flat; [brush] and [sweep] are the full run, for the few marks
 * big enough to show a gradient. [style] decides how the accent is spent.
 */
object Accent {
    var colors: AccentColors by mutableStateOf(AccentColors.Icon)

    /** The style in force — see [AccentStyle]. */
    var style: AccentStyle by mutableStateOf(AccentStyle.SUBTLE)

    /** How much of the usual accent wash goes over cards, menus and drawers. */
    val washScale: Float get() = when (style) {
        AccentStyle.GLOW -> 1.4f
        AccentStyle.DUOTONE -> 1.8f
        AccentStyle.NEON -> 0f
        else -> 1f
    }

    /** The fill of a selected chip, segment or switch, as one colour. */
    val Fill: Color get() = if (style == AccentStyle.DEEP) Start.withLightness(DEEP_FILL_LIGHTNESS) else Start

    /** [Fill] as a brush: the full gradient under Glow. */
    val fillBrush: Brush get() = when (style) {
        AccentStyle.GLOW -> brush
        else -> SolidColor(Fill)
    }

    /** Neon draws a selected chip or segment as an outline rather than a fill. */
    val outlined: Boolean get() = style == AccentStyle.NEON

    /** Text or a glyph drawn on [Fill] (or inside Neon's outline). */
    val OnFill: Color get() = when (style) {
        AccentStyle.NEON -> Icon
        AccentStyle.DEEP, AccentStyle.GLOW -> Color.White
        else -> if (Fill.luminance() > 0.55f) Color.Black else Color.White
    }

    /** A plain icon drawn in the accent on a surface. */
    val Icon: Color get() = when (style) {
        AccentStyle.SUBTLE -> Start
        AccentStyle.DEEP -> Start.withLightness(0.80f)
        AccentStyle.GLOW -> Color.White
        AccentStyle.DUOTONE -> Start.withLightness(0.68f)
        AccentStyle.NEON -> Start.withLightness(0.66f)
    }

    /** Accent-coloured headline text, such as Home's greeting. */
    val headlineBrush: Brush get() = if (style == AccentStyle.NEON) brush else SolidColor(Icon)

    /** The round tile behind a Settings icon. */
    val tileBrush: Brush get() = when (style) {
        AccentStyle.DEEP -> SolidColor(Start.withLightness(0.30f))
        AccentStyle.DUOTONE -> SolidColor(Start)
        AccentStyle.NEON -> SolidColor(Color.Transparent)
        else -> brush
    }

    /** The glyph on [tileBrush]. */
    val TileGlyph: Color get() = when (style) {
        AccentStyle.DEEP -> Start.withLightness(0.82f)
        AccentStyle.DUOTONE -> OnSolid
        AccentStyle.NEON -> Icon
        else -> Color.White
    }

    /** How thick Neon's outlines are drawn. */
    val OutlineWidth: Dp = 1.dp

    /** An outline drawn round tiles, the mini player and menus, if the style has one. */
    val Outline: Color? get() = if (style == AccentStyle.NEON) Start else null

    /**
     * The mini player's second line (the artist), given the [default] colour
     * and the plain [container]: lifted to a soft white on Glow's gradient,
     * where the usual grey disappears into it.
     */
    fun barSubtitle(default: Color, container: Color): Color =
        if (style == AccentStyle.GLOW && container.luminance() <= 0.5f) Color.White.copy(alpha = 0.82f) else default

    /** The mini player's fill, given the plain [container] it would otherwise have. */
    fun barBrush(container: Color): Brush = when {
        container.luminance() > 0.5f -> SolidColor(container)
        style == AccentStyle.GLOW -> brush
        style == AccentStyle.DUOTONE -> SolidColor(lerp(container, Start, 0.28f))
        else -> SolidColor(container)
    }

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

/**
 * A selected chip, segment or button in the accent, as the style draws it:
 * filled with [Accent.fillBrush], or outlined under Neon.
 */
fun Modifier.accentFill(shape: Shape): Modifier =
    if (Accent.outlined) {
        border(Accent.OutlineWidth, Accent.Start, shape)
    } else {
        background(Accent.fillBrush, shape)
    }

/**
 * A round icon tile (already clipped to its circle) as the style draws it —
 * the same tile Settings uses: [Accent.tileBrush], ringed under Neon.
 */
fun Modifier.accentTile(): Modifier =
    background(Accent.tileBrush).then(
        Accent.Outline?.let { border(Accent.OutlineWidth, it, CircleShape) } ?: Modifier,
    )

/** The outline round the selected card in a list: the gradient, or Neon's thin line. */
fun Modifier.accentSelectedBorder(shape: Shape): Modifier =
    Accent.Outline?.let { border(Accent.OutlineWidth, it, shape) } ?: border(1.dp, Accent.brush, shape)

/** Glow's soft light of the accent across the top of the page; nothing in any other style. */
fun Modifier.accentGlow(dark: Boolean): Modifier =
    if (Accent.style != AccentStyle.GLOW || !dark) {
        this
    } else {
        drawBehind {
            val start = Accent.Start
            val end = Accent.End
            val radius = maxOf(size.width * 1.05f, size.height * 0.55f)
            drawRect(
                Brush.radialGradient(
                    0f to start.copy(alpha = 0.42f),
                    0.45f to end.copy(alpha = 0.20f),
                    1f to Color.Transparent,
                    center = Offset(size.width / 2f, 0f),
                    radius = radius,
                ),
            )
        }
    }
