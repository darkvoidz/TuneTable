package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.settings.PlayerTheme
import com.music.bitchord.ui.theme.Accent
import com.music.bitchord.ui.theme.AccentStyle
import com.music.bitchord.ui.theme.withLightness

/*
 * Settings → Appearance's pickers as small phones rather than words: each
 * option drawn as a miniature of what it gives — the Home screen for Style, the now playing screen for Player style — in the listener's own accent.
 */

private val THUMB_WIDTH = 76.dp
private val THUMB_HEIGHT = 136.dp
private val THUMB_SHAPE = RoundedCornerShape(14.dp)
private val SMALL = RoundedCornerShape(3.dp)

/** A sideways-scrolling row of previews, each tappable, the chosen one ringed in the accent. */
@Composable
internal fun <T> PreviewPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    preview: @Composable (T) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { option ->
            val chosen = option == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (!chosen) onSelect(option) },
            ) {
                Box(
                    Modifier
                        .border(
                            if (chosen) 2.dp else 1.dp,
                            if (chosen) Accent.Start else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(17.dp),
                        )
                        .padding(3.dp)
                        .size(THUMB_WIDTH, THUMB_HEIGHT)
                        .clip(THUMB_SHAPE),
                ) { preview(option) }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label(option),
                    fontSize = 12.sp,
                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                    color = if (chosen) Accent.Start else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** The colours a style dresses Home in, for a preview — dark theme. */
private class StyleColors(
    val page: Color,
    val card: Color,
    val chip: Brush,
    val chipOutline: Boolean,
    val bar: Brush,
    val outline: Color?,
    val headline: Color,
    val glow: Boolean,
)

private fun styleColors(style: AccentStyle): StyleColors {
    val start = Accent.Start
    val end = Accent.End
    val grey = Color(0xFF1C1C1E)
    return when (style) {
        AccentStyle.SUBTLE -> StyleColors(
            lerp(Color.Black, start, 0.07f), lerp(grey, start, 0.12f), Brush.linearGradient(listOf(start, start)),
            false, Brush.linearGradient(listOf(lerp(grey, start, 0.12f), lerp(grey, start, 0.12f))), null, start, false,
        )
        AccentStyle.DEEP -> {
            val card = start.withLightness(0.13f)
            StyleColors(
                start.withLightness(0.055f), card, Brush.linearGradient(listOf(start.withLightness(0.42f), start.withLightness(0.42f))),
                false, Brush.linearGradient(listOf(card, card)), null, start.withLightness(0.8f), false,
            )
        }
        AccentStyle.GLOW -> StyleColors(
            lerp(Color.Black, end, 0.03f), Color.White.copy(alpha = 0.09f), Accent.brush,
            false, Accent.brush, null, Color.White, true,
        )
        AccentStyle.DUOTONE -> {
            val card = lerp(grey, end, 0.22f)
            StyleColors(
                lerp(Color.Black, end, 0.12f), card, Brush.linearGradient(listOf(start, start)),
                false, Brush.linearGradient(listOf(card, card)), null, start.withLightness(0.68f), false,
            )
        }
        AccentStyle.NEON -> StyleColors(
            Color(0xFF060606), Color(0xFF121214), Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            true, Brush.linearGradient(listOf(Color(0xFF121214), Color(0xFF121214))), start, start.withLightness(0.66f), false,
        )
    }
}

/** Home in [style], as a miniature. */
@Composable
internal fun AppStylePreview(style: AccentStyle) {
    val c = styleColors(style)
    MiniHome(
        page = c.page, card = c.card, text = Color.White, headline = c.headline,
        chip = c.chip, chipOutline = if (c.chipOutline) Accent.Start else null,
        bar = c.bar, outline = c.outline, glow = c.glow,
    )
}

@Composable
private fun MiniHome(
    page: Color,
    card: Color,
    text: Color,
    headline: Color,
    chip: Brush,
    chipOutline: Color?,
    bar: Brush,
    outline: Color?,
    glow: Boolean,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(page)
            .then(if (glow) Modifier.accentGlowPreview() else Modifier),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Box(Modifier.size(30.dp, 4.dp).clip(SMALL).background(headline))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(46.dp, 7.dp).clip(SMALL).background(text))
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    Modifier
                        .size(15.dp, 7.dp)
                        .clip(CircleShape)
                        .background(chip)
                        .then(if (chipOutline != null) Modifier.border(0.75.dp, chipOutline, CircleShape) else Modifier),
                )
                repeat(2) { Box(Modifier.size(15.dp, 7.dp).clip(CircleShape).background(card)) }
            }
            Spacer(Modifier.height(7.dp))
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) { Box(Modifier.size(28.dp, 13.dp).clip(SMALL).background(card)) }
                }
                Spacer(Modifier.height(3.dp))
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MiniBar(width = 66.dp, brush = bar, outline = outline)
            Spacer(Modifier.height(3.dp))
            MiniBar(width = 48.dp, brush = Brush.linearGradient(listOf(card, card)), outline = outline)
        }
    }
}

@Composable
private fun MiniBar(width: Dp, brush: Brush, outline: Color?) {
    Box(
        Modifier
            .size(width, 10.dp)
            .clip(CircleShape)
            .background(brush)
            .then(if (outline != null) Modifier.border(0.75.dp, outline, CircleShape) else Modifier),
    )
}

/** Glow's light across the top of a preview. */
private fun Modifier.accentGlowPreview(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            0f to Accent.Start.copy(alpha = 0.45f),
            0.5f to Accent.End.copy(alpha = 0.18f),
            1f to Color.Transparent,
            center = Offset(size.width / 2f, 0f),
            radius = size.height * 0.6f,
        ),
    )
}

/** The now playing screen for [theme], as a miniature; Follow shows the app's style, tagged with its name. */
@Composable
internal fun PlayerStylePreview(theme: PlayerTheme, followLabel: String) {
    val style = when (theme) {
        PlayerTheme.FOLLOW -> Accent.style
        PlayerTheme.SUBTLE, PlayerTheme.SIMPLE -> AccentStyle.SUBTLE
        PlayerTheme.DEEP -> AccentStyle.DEEP
        PlayerTheme.GLOW -> AccentStyle.GLOW
        PlayerTheme.DUOTONE -> AccentStyle.DUOTONE
        PlayerTheme.NEON -> AccentStyle.NEON
    }
    val start = Accent.Start
    val end = Accent.End
    val cover = Color(0xFF8A2A3A)
    val coverArt = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFBE123C), Color(0xFF4C1D95)))
    val background: Brush = when {
        theme == PlayerTheme.SIMPLE -> Brush.verticalGradient(listOf(Color(0xFFB4453F), Color(0xFF6B2A4A), Color(0xFF3A1F3F)))
        style == AccentStyle.SUBTLE -> Brush.verticalGradient(listOf(cover, Color(0xFF2A0F14), Color(0xFF0B0B0C)))
        style == AccentStyle.DEEP -> Brush.verticalGradient(
            listOf(lerp(cover, start.withLightness(0.2f), 0.5f), start.withLightness(0.06f)),
        )
        style == AccentStyle.DUOTONE -> Brush.verticalGradient(
            listOf(lerp(cover, end, 0.45f), lerp(Color(0xFF08080C), end, 0.15f)),
        )
        style == AccentStyle.GLOW -> Brush.verticalGradient(listOf(Color(0xFF07080A), Color(0xFF07080A)))
        else -> Brush.verticalGradient(listOf(Color(0xFF060606), Color(0xFF060606)))
    }
    val plain = theme == PlayerTheme.SIMPLE
    val icon = if (plain) Color.White else Accent.iconFor(style)
    val glyph = when (style) {
        AccentStyle.SUBTLE, AccentStyle.GLOW, AccentStyle.DUOTONE -> Color.White
        else -> icon
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .then(
                if (style == AccentStyle.GLOW && !plain) {
                    Modifier.drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                0f to start.copy(alpha = 0.5f),
                                0.55f to end.copy(alpha = 0.18f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height * 0.3f),
                                radius = size.width * 0.9f,
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(coverArt)
                    .then(if (style == AccentStyle.NEON && !plain) Modifier.border(0.75.dp, start, RoundedCornerShape(5.dp)) else Modifier),
            )
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth()) {
                Box(Modifier.size(40.dp, 5.dp).clip(SMALL).background(Color.White))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(26.dp, 4.dp).clip(SMALL).background(Color.White.copy(alpha = 0.55f)))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(0.4f).height(2.dp).background(Color.White))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(SMALL).background(glyph.copy(alpha = 0.85f)))
                val disc = Modifier.size(20.dp).clip(CircleShape)
                Box(
                    when {
                        plain || style == AccentStyle.SUBTLE -> disc.background(Color.White)
                        style == AccentStyle.DEEP -> disc.background(start.withLightness(0.42f))
                        style == AccentStyle.GLOW -> disc.background(Accent.brush)
                        style == AccentStyle.DUOTONE -> disc.background(start)
                        else -> disc.border(0.75.dp, start, CircleShape)
                    },
                )
                Box(Modifier.size(7.dp).clip(SMALL).background(glyph.copy(alpha = 0.85f)))
            }
        }
        if (theme == PlayerTheme.FOLLOW) {
            Text(
                text = followLabel,
                fontSize = 8.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}
