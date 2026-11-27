package com.music.bitchord.ui.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.theme.Accent

/**
 * Neon's player glyphs: drawn as thin hollow lines rather than solid shapes,
 * to match the style's outlines. Each is a 24-unit stroke drawing, tinted by
 * whatever draws it.
 */
internal object NeonGlyphs {
    val Previous by lazy { line("neon_previous", "M11.5 6.5v11L4 12zM20 6.5v11L12.5 12z") }
    val Next by lazy { line("neon_next", "M4 6.5v11l7.5-5.5zM12.5 6.5v11L20 12z") }
    val Play by lazy { line("neon_play", "M8.5 6.5v11l8.5-5.5z") }
    val Pause by lazy { line("neon_pause", "M7.5 6h2.5v12H7.5zM14 6h2.5v12H14z") }
    val Shuffle by lazy {
        line(
            "neon_shuffle",
            "M3 7h3.5c2 0 3 1 4.5 3.5l2 3.5c1.5 2.5 2.5 3.5 4.5 3.5H21M18 14.5l3 3-3 3" +
                "M3 17h3.5c1.3 0 2.2-.5 3-1.4M13.5 8.4c.8-.9 1.7-1.4 3-1.4H21M18 4l3 3-3 3",
        )
    }
    val Repeat by lazy {
        line("neon_repeat", "M4 11V9.5A2.5 2.5 0 0 1 6.5 7H19l-2.5-2.5M20 13v1.5a2.5 2.5 0 0 1-2.5 2.5H5l2.5 2.5")
    }
    val AirPlay by lazy { line("neon_airplay", "M7.5 16.5a6.5 6.5 0 1 1 9 0M9.6 14.2a3.6 3.6 0 1 1 4.8 0M8 21h8l-4-4.5z") }
    val Lyrics by lazy { line("neon_lyrics", "M4 5h16v11H9l-4 3.5V16H4zM9.5 9v2M14 9v2") }
    val Queue by lazy { line("neon_queue", "M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01") }

    private fun line(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}

/** This glyph as Neon draws it — its hollow line version — or itself in any other style. */
internal fun ImageVector.neon(): ImageVector {
    if (!Accent.outlined) return this
    return when (this) {
        BitChordIcons.Shuffle -> NeonGlyphs.Shuffle
        BitChordIcons.Repeat -> NeonGlyphs.Repeat
        BitChordIcons.AirPlay -> NeonGlyphs.AirPlay
        BitChordIcons.LyricsQuote -> NeonGlyphs.Lyrics
        BitChordIcons.Queue -> NeonGlyphs.Queue
        Icons.Rounded.RepeatOne -> Icons.Outlined.RepeatOne
        Icons.Rounded.Headphones -> Icons.Outlined.Headphones
        Icons.Rounded.CastConnected -> Icons.Outlined.CastConnected
        Icons.Rounded.DirectionsCar -> Icons.Outlined.DirectionsCar
        Icons.Rounded.Tv -> Icons.Outlined.Tv
        Icons.Rounded.Watch -> Icons.Outlined.Watch
        else -> this
    }
}
