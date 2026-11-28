package com.music.bitchord.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.music.bitchord.data.settings.AccentMode
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.AccentColors
import com.music.bitchord.ui.theme.AccentStyle
import com.music.bitchord.ui.theme.SystemAccents
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb

/**
 * How the widgets look in each of the app's Styles, worked out from the
 * accent colour chosen in Settings — the same choices the app itself makes,
 * in a form a widget's views can take.
 */
internal object MediaWidgetStyle {

    /** Every colour a widget needs, as ARGB. */
    class Palette(
        val style: AccentStyle,
        val start: Int,
        val end: Int,
        /** The card's fill, or the two ends of its gradient. */
        val cardTop: Int,
        val cardBottom: Int,
        /** A soft accent bloom in the card (Glow). */
        val glow: Boolean,
        val border: Int,
        val borderWidthDp: Float,
        /** The play disc: solid, a gradient (Glow), or a ring (Neon). */
        val playFrom: Int,
        val playTo: Int,
        val playRing: Boolean,
        val playGlyph: Int,
        val icon: Int,
        /** Like when liked, shuffle when on. */
        val active: Int,
        val title: Int,
        val subtitle: Int,
        /** Neon's hollow transport glyphs rather than solid ones. */
        val lineGlyphs: Boolean,
    )

    /** The palette for the Style and accent in Settings right now. */
    fun current(context: Context): Palette {
        val accent = when (AppSettings.accentMode.value) {
            AccentMode.ICON -> AccentColors.Icon
            AccentMode.COLOR -> AccentColors.single(ComposeColor(AppSettings.accentColor.value))
            AccentMode.SYSTEM -> SystemAccents.colors(context).getOrNull(AppSettings.accentSystemIndex.value)
                ?.let(AccentColors::single)
                ?: AccentColors.Icon
        }
        return palette(AppSettings.accentStyle.value, accent.start.toArgb(), accent.end.toArgb())
    }

    fun palette(style: AccentStyle, start: Int, end: Int): Palette {
        val white = Color.WHITE
        val dim = 0xB3FFFFFF.toInt()
        val hairline = 0x14FFFFFF
        return when (style) {
            AccentStyle.SUBTLE -> Palette(
                style, start, end,
                cardTop = mix(BASE, start, 0.14f), cardBottom = mix(BASE, start, 0.14f), glow = false,
                border = hairline, borderWidthDp = 1f,
                playFrom = start, playTo = start, playRing = false, playGlyph = onColor(start),
                icon = white, active = start, title = white, subtitle = dim, lineGlyphs = false,
            )
            AccentStyle.DEEP -> {
                val pale = lightness(start, 0.80f)
                Palette(
                    style, start, end,
                    cardTop = lightness(start, 0.14f), cardBottom = lightness(start, 0.10f), glow = false,
                    border = hairline, borderWidthDp = 1f,
                    playFrom = lightness(start, 0.42f), playTo = lightness(start, 0.42f), playRing = false,
                    playGlyph = white, icon = pale, active = pale,
                    title = white, subtitle = ColorUtils.setAlphaComponent(pale, 0xCC), lineGlyphs = false,
                )
            }
            AccentStyle.GLOW -> Palette(
                style, start, end,
                cardTop = GLOW_BASE, cardBottom = GLOW_BASE, glow = true,
                border = hairline, borderWidthDp = 1f,
                playFrom = start, playTo = end, playRing = false, playGlyph = white,
                icon = white, active = start, title = white, subtitle = dim, lineGlyphs = false,
            )
            AccentStyle.DUOTONE -> Palette(
                style, start, end,
                cardTop = mix(BASE, start, 0.22f), cardBottom = mix(BASE, end, 0.30f), glow = false,
                border = hairline, borderWidthDp = 1f,
                playFrom = start, playTo = start, playRing = false, playGlyph = onColor(start),
                icon = white, active = start, title = white, subtitle = dim, lineGlyphs = false,
            )
            AccentStyle.NEON -> {
                val neon = lightness(start, 0.66f)
                Palette(
                    style, start, end,
                    cardTop = NEON_BASE, cardBottom = NEON_BASE, glow = false,
                    border = ColorUtils.setAlphaComponent(neon, 0xB3), borderWidthDp = 1f,
                    playFrom = neon, playTo = neon, playRing = true, playGlyph = neon,
                    icon = neon, active = neon, title = white, subtitle = dim, lineGlyphs = true,
                )
            }
        }
    }

    /** The card, at the widget's size: its fill, any glow, and its edge. */
    fun card(p: Palette, widthDp: Int, heightDp: Int, density: Float, cornerRadiusDp: Float, capsule: Boolean): Bitmap {
        // Bounded: every bitmap is parcelled to the launcher, which keeps it.
        val scale = minOf(density, MAX_CARD_PX / maxOf(widthDp, heightDp).toFloat())
        val w = (widthDp * scale).roundToInt().coerceAtLeast(1)
        val h = (heightDp * scale).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val inset = p.borderWidthDp * scale / 2f
        val rect = RectF(inset, inset, w - inset, h - inset)
        val radius = if (capsule) rect.height() / 2f else minOf(cornerRadiusDp * scale, rect.height() / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), p.cardTop, p.cardBottom, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (p.glow) {
            // Two blooms, the accent's two ends, clipped to the card.
            canvas.save()
            canvas.clipPath(android.graphics.Path().apply { addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW) })
            val r = maxOf(w, h) * 0.75f
            paint.shader = RadialGradient(w * 0.15f, h * 0.1f, r, ColorUtils.setAlphaComponent(p.start, 0x66), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(rect, paint)
            paint.shader = RadialGradient(w * 0.95f, h * 1.0f, r, ColorUtils.setAlphaComponent(p.end, 0x4D), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(rect, paint)
            canvas.restore()
        }

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = p.borderWidthDp * scale
        paint.color = p.border
        canvas.drawRoundRect(rect, radius, radius, paint)
        return out
    }

    /** The play button's disc: a fill, a gradient, or Neon's ring. */
    fun playDisc(p: Palette, sizeDp: Int, density: Float): Bitmap {
        val px = (sizeDp * density).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val c = px / 2f
        if (p.playRing) {
            val stroke = 1.5f * density
            paint.style = Paint.Style.FILL
            paint.color = ColorUtils.setAlphaComponent(p.playFrom, 0x1F)
            canvas.drawCircle(c, c, c - stroke, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = p.playFrom
            canvas.drawCircle(c, c, c - stroke / 2f, paint)
        } else {
            paint.shader = LinearGradient(0f, 0f, px.toFloat(), px.toFloat(), p.playFrom, p.playTo, Shader.TileMode.CLAMP)
            canvas.drawCircle(c, c, c, paint)
        }
        return out
    }

    /** Black or white, whichever reads on [background]. */
    private fun onColor(background: Int): Int =
        if (ColorUtils.calculateLuminance(background) > 0.5) 0xFF111111.toInt() else Color.WHITE

    private fun mix(a: Int, b: Int, t: Float): Int = ColorUtils.blendARGB(a, b, t)

    /** [color] at HSL lightness [l], keeping its hue and saturation. */
    private fun lightness(color: Int, l: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = l
        return ColorUtils.HSLToColor(hsl)
    }

    private const val BASE = 0xFF1C1C1E.toInt()
    private const val GLOW_BASE = 0xFF0B0C10.toInt()
    private const val NEON_BASE = 0xEB0A0A0C.toInt()
    private const val MAX_CARD_PX = 1_200f
}
