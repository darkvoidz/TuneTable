package com.music.bitchord.ui.components

import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.ExperimentalHazeApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * The strip the tab bar alone needs, above the gesture inset. Generous on
 * purpose: the ramp below spends most of its run at an alpha too low to see,
 * and that long invisible lead-in is what hides where the layer begins.
 */
private val FADE_HEIGHT = 180.dp

/**
 * Taller once the mini player is stacked on top of the tab bar — by the 56dp
 * the pill-shaped bar now stands, plus the 8dp gap above it and the run the
 * ramp wants over both.
 */
private val FADE_HEIGHT_WITH_MINI_PLAYER = 254.dp

/**
 * How many colour stops the ramp is cut into.
 *
 * A two-stop gradient is interpolated linearly in the shader, which is both the
 * wrong curve and — across a strip this tall in a near-flat colour — enough of
 * a straight line through the low alphas to band visibly on an 8-bit display.
 * Sampling the curve at intervals hands the shader short spans to interpolate
 * across instead, and the banding goes with them.
 */
private const val STOPS = 16

/**
 * The floor the floating bars sit on: the page's own colour, faded in from
 * nothing at the top to solid at the very bottom.
 *
 * Flat colour rather than glass, deliberately. A blur here has to sample the
 * feed scrolling under it, and what it costs is paid on every frame of that
 * scroll; a gradient costs one shader over a fixed rect. The bars themselves
 * still carry glass, so the frosted look survives where it is actually read —
 * on the pill and the mini player, against a floor that is only ever a wash of
 * the colour already behind them.
 *
 * The ramp is the feathering. The top edge is fully transparent, which is what
 * keeps the strip from reading as a rectangle stuck over the feed, and the side
 * edges run to the screen edges, so they have no seam of their own to soften.
 *
 * Unlike the blur it replaced, this stays on under Reduce dynamic blur: it is
 * not a blur, it costs nothing to leave in, and with the bars filled solid it
 * is the only thing keeping content from running out from under them.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun BottomFadeScrim(
    modifier: Modifier = Modifier,
    withMiniPlayer: Boolean = false,
    /**
     * The colour to fade in — whatever the page is painting at the foot of the
     * screen. The theme's background on a tab, and on a detail page the tint
     * its wash has settled into down here rather than the wash itself.
     */
    pageColor: Color = MaterialTheme.colorScheme.background,
    /** The page's haze; with it the strip blurs what is under it as well as fading it. */
    hazeState: HazeState? = null,
) {
    // The gesture bar sits below the tab pill and wants covering too, so it is
    // added on rather than being part of the fade's own run.
    val inset = floatingBarInsets.asPaddingValues().calculateBottomPadding()
    val blurred = hazeState != null && isGlassSupported()
    val height by animateDpAsState(
        targetValue = inset + when {
            blurred && withMiniPlayer -> BLUR_HEIGHT_WITH_MINI_PLAYER
            blurred -> BLUR_HEIGHT
            withMiniPlayer -> FADE_HEIGHT_WITH_MINI_PLAYER
            else -> FADE_HEIGHT
        },
        // Matches the beat the mini player takes to appear, so the floor grows
        // with it instead of snapping ahead of it.
        animationSpec = tween(220),
        label = "bottomScrimHeight",
    )

    val brush = remember(pageColor) {
        Brush.verticalGradient(
            // A cubic ease-in rather than a straight ramp: the alpha then holds
            // under a few percent for the first half of the strip, which is what
            // stops the eye from finding the line where the layer starts. Its
            // whole run is spent arriving — the same curve the blur it replaced
            // ramped its radius along, so the strip reads at the same weight.
            colorStops = Array(STOPS) { i ->
                val t = i / (STOPS - 1f)
                t to pageColor.copy(alpha = EaseInCubic.transform(t))
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (blurred && hazeState != null) {
                    // A progressive blur under the fade: what scrolls down
                    // behind the bars is frosted, hardest at the foot of the
                    // screen and easing to nothing at the top of the strip.
                    // The fade over it is lighter, since the blur already
                    // keeps the page from competing with the bars.
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(HeadFade, blendMode = BlendMode.DstIn)
                            drawRect(brush, alpha = BLURRED_SCRIM_ALPHA)
                        }
                        .hazeEffect(state = hazeState) {
                            backgroundColor = pageColor
                            tints = emptyList()
                            blurRadius = BLUR_MAX
                            noiseFactor = 0f
                            inputScale = HazeInputScale.Fixed(0.33f)
                            progressive = HazeProgressive.verticalGradient(
                                easing = EaseInOut,
                                startIntensity = 0f,
                                endIntensity = 1f,
                            )
                        }
                } else {
                    Modifier.background(brush)
                },
            ),
    )
}

/**
 * The blurred strip's height above the gesture bar: from the foot of the
 * screen to halfway up the mini player — the tab pill's lift, the pill, the
 * gap over it and half the mini player — so only the lower half of the mini
 * player has the page frosted behind it.
 */
private val BLUR_HEIGHT_WITH_MINI_PLAYER = 104.dp

/** Without a mini player: halfway up the tab pill. */
private val BLUR_HEIGHT = 34.dp

/** The blur at the very foot of the screen, where it is strongest. */
private val BLUR_MAX = 24.dp

/** How much of the colour fade stays over the blur. */
private const val BLURRED_SCRIM_ALPHA = 0.55f

/** The blurred strip's opacity: eased in from nothing over its top third, then whole. */
private val HeadFade = Brush.verticalGradient(
    colorStops = Array(9) { i ->
        val t = i / 8f
        (t * 0.35f) to Color.Black.copy(alpha = EaseInOut.transform(t))
    },
)

/**
 * How far above the floating bars the page stops taking touches. A tap that
 * just misses the top of the mini player belongs to it, not to whatever row
 * the feed happens to have scrolled under there.
 */
private val TAP_GUARD_MARGIN = 8.dp

/**
 * A dead strip across the foot of the screen, as tall as the floating bars
 * plus [TAP_GUARD_MARGIN], so a thumb that misses a tab or the mini player
 * lands on nothing rather than on a song or an album passing under the bars —
 * which played it, or opened it, out of a tap meant for the bar.
 *
 * Drawn after the page and before the bars, so the bars still take their own
 * touches and the page gets none here. Full width, gaps included: the space
 * beside the pill and between the bars is where a near miss lands. Scrolls
 * started on the strip are swallowed too — the page scrolls from anywhere
 * above it — which is how a tab bar behaves on iOS as well.
 *
 * Only the bottom: the fade at the top, over the status bar, guards nothing.
 *
 * [barsHeight] is read in layout only. The glass bar changes height on every
 * frame of its fold, and read in composition that would recompose the whole
 * app root along with it.
 */
@Composable
fun FloatingBarsTapGuard(
    barsHeight: () -> Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val bars = barsHeight()
                val height = if (bars <= 0) 0 else (bars + TAP_GUARD_MARGIN.roundToPx())
                val placeable = measurable.measure(
                    Constraints.fixed(
                        width = constraints.maxWidth,
                        height = height.coerceAtMost(constraints.maxHeight),
                    ),
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .pointerInput(Unit) {
                // Being hit at all is what keeps the page underneath from
                // being hit. Consumed as well, so nothing above it in the tree
                // reads a tap out of it either.
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
    )
}
