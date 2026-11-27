package com.music.bitchord.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * TuneTable's accent: the app icon's coral running to purple.
 *
 * [Solid] is the coral alone, for anything drawn in one colour — a lit glyph,
 * a switch's track, a check mark. [brush] is the full run, for the few marks
 * big enough to show a gradient: the selected tab, the mini player's ring.
 */
object Accent {
    val Start = Color(0xFFFF7A59)
    val End = Color(0xFF7B2FF7)
    val Solid = Start

    /** The coral-to-purple run, corner to corner across whatever it fills. */
    val brush: Brush get() = Brush.linearGradient(listOf(Start, End))

    /** The same run as an angle sweep, for a ring. */
    val sweep: Brush get() = Brush.sweepGradient(listOf(Start, End, Start))
}
