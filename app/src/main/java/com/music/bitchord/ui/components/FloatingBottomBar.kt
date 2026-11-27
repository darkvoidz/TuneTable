package com.music.bitchord.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import com.music.bitchord.ui.theme.Accent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.roundToInt

data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * The gap between the pill's glass edge and the tabs inside it.
 *
 * Apple's selection sits about 4pt in from its tab bar's edge, measured off
 * the WWDC Liquid Glass frames; it was 8 here, then 6.
 *
 * Shared with [GlassNavBar], which is meant to measure the same as this bar
 * rather than merely near it.
 */
internal val PILL_INSET = 4.dp

/**
 * The tab bar's margin at either side of the screen: Apple's is 19pt, wider
 * than the page's own [BAR_GUTTER].
 */
internal val NAV_BAR_GUTTER = 19.dp

/**
 * How far above [floatingBarInsets] the bar sits: on a gesture inset of 15dp
 * this puts its foot 21dp off the screen's edge, where Apple's is.
 */
internal val NAV_BAR_LIFT = 6.dp

/**
 * How far the floating bars stand off the bottom edge: the system navigation
 * bar's inset, but never less than a gesture bar's.
 *
 * With the gesture hint (or the navigation bar itself) switched off that inset
 * is zero, and the tab bar dropped onto the screen's very edge. It keeps the
 * height it has with the hint showing instead. Three-button navigation is
 * taller than the floor, so it still wins there.
 */
internal val floatingBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

/** A gesture bar's inset — 45px at the test phone's 3x density. */
private val GESTURE_BAR_FLOOR = 15.dp

/**
 * Each tab's own vertical padding, and the counterweight to [PILL_INSET].
 *
 * The pill has no height of its own — it is whatever its contents come to — so
 * taking 2dp off the inset above would have shortened the whole bar by 4. The
 * same 2dp is added back here instead, which leaves the bar's outer height
 * exactly where it was and moves the boundary rather than the bar. The two
 * numbers are a pair: change one and the bar's height moves unless the other
 * moves against it.
 */
internal val TAB_VERTICAL_PADDING = 6.5.dp

/** The gap between a tab's glyph and its label, in both bars. */
internal val TAB_ICON_LABEL_GAP = 1.dp

/**
 * A tab's glyph box. Apple's glyphs stand 21 to 24pt in a bar 61pt tall, a
 * larger share of it than these had at 25dp in 70; at 28 they come out at
 * Apple's sizes, and with the padding above the bar is 62dp.
 */
internal val TAB_ICON_SIZE = 28.dp

/** A tab's label: Apple's 10pt, letter-spaced as SF Pro Text sets it. */
@Composable
internal fun tabLabelStyle(): TextStyle =
    MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp)

/**
 * The spring the selection indicator and the tab glyphs both travel on.
 *
 * Damping 0.72 rather than the 0.5 it was: half-damped overshoots two or three
 * times, and a run of diminishing bounces is what makes a control read as a
 * spring rather than as a material. This settles on the second approach — one
 * soft pass beyond the mark and done — which is the difference between bouncy
 * and alive.
 *
 * Stiffness 320 puts the whole movement at roughly a third of a second, quick
 * enough that the tap and the arrival feel like one event.
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * How far the indicator elongates along its travel, at full stride.
 *
 * This is the part that reads as liquid rather than as a sliding rectangle. A
 * shape crossing a gap under its own momentum does not stay the shape it was:
 * it draws out along the direction it is going and gathers itself back at the
 * end. Driven off how far there is still to go, so it is widest in the middle
 * of the trip and exactly itself once it arrives — no state to keep, and it
 * falls out of a drag for free, since dragging is nothing but a long way still
 * to go.
 *
 * Sixteen percent is enough to be felt and not enough to be caught at: past
 * about a fifth the pill starts reading as a stretched image of itself.
 */
internal const val STRETCH = 0.16f

/**
 * How much of the stretch is taken back out of the indicator's height.
 *
 * Half, not all. Conserving area exactly is what a drop of water does, and it
 * is too much here — the indicator sits behind a glyph that is not deforming
 * with it, and a full counter-squash reads as the pill being crushed rather
 * than drawn. Half keeps the sense of something with a volume to redistribute
 * while leaving the glyph its ground.
 */
internal const val SQUASH = 0.5f

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    /** Called when the user taps the currently active tab (scroll-to-top). */
    onActiveTabTap: () -> Unit = {},
    /**
     * Folded down to the current tab alone, as it is while a page is being
     * scrolled down — the state the bar is headed for. [fold] is how far
     * along the way it is, 0 open to 1 folded, and drives the drawing.
     */
    collapsed: Boolean = false,
    fold: () -> Float = { if (collapsed) 1f else 0f },
    /** Asked for by a tap on the folded bar: bring the other tabs back. */
    onExpand: () -> Unit = {},
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surfaceVariant
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    // The glass settle is exactly the motion "reduce animation" promises to
    // drop — snapping both the indicator's travel and the glyph's pop to
    // their target leaves the tap itself instant rather than eased.
    val glassSpec: AnimationSpec<Float> = if (reduceAnimation) snap() else GlassSpring

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { TAB_GAP.toPx() }
    val n = tabs.size

    // Every tab is the same fixed circle, [TAB_SLOT] across, [TAB_GAP] apart,
    // so the bar is only as wide as its icons and tab i starts at i steps.
    val tabWidthPx = if (rowSize.width > 0 && n > 0) with(density) { TAB_SLOT.toPx() } else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) tabWidthPx + gapPx else 0f

    // The drag below runs in one coroutine for the life of the bar, so it
    // reads everything that can change under it through these. Read plainly,
    // each would stay what it was on the first touch.
    val currentTabStepPx by rememberUpdatedState(tabStepPx)
    val currentLastIndex by rememberUpdatedState(tabs.lastIndex)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    val pillTargetPx = if (tabStepPx > 0f) {
        selectedIndex * tabStepPx + dragOffset
    } else 0f

    val animatedPillOffset by animateFloatAsState(
        targetValue = pillTargetPx,
        animationSpec = glassSpec,
        label = "pillOffset",
    )

    // How much of a tab's stride is still ahead of the indicator: 0 at rest,
    // toward 1 in the middle of a move or under a drag that has run away from
    // it. The stretch below is a function of this and nothing else, which is
    // what keeps it honest — the shape can only be deformed while it is
    // actually behind where it is going.
    val lag = if (tabStepPx > 0f) {
        (abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        dragOffset = 0f
        lastHapticTab = selectedIndex
    }

    Box(
        modifier = modifier
            .windowInsetsPadding(floatingBarInsets)
            .padding(bottom = NAV_BAR_LIFT)
            // Sized to its icons and centred by the column it sits in, rather
            // than run across the screen.
            .clip(pillShape)
            .then(
                if (neonFrost) {
                    // Neon's frosted charcoal, as its menus have.
                    Modifier.optimizedHazeEffect(state = hazeState, style = menuFrost(NEON_CHARCOAL, NEON_FROST_ALPHA))
                } else if (reduceDynamicBlur) {
                    Modifier.background(container)
                } else if (useGlass) {
                    Modifier.liquidGlass(shape = pillShape)
                } else {
                    Modifier.optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.regular(container),
                    )
                },
            )
            .border(if (Accent.Outline != null) Accent.OutlineWidth else GLASS_EDGE_WIDTH, Accent.Outline ?: GLASS_EDGE_COLOR, pillShape)
            .padding(horizontal = PILL_INSET, vertical = PILL_INSET),
    ) {
    // Folding: the row keeps its full size but only part of it is shown — from
    // the whole row open, down to one tab's circle folded — and it slides so the
    // selected tab ends up in that circle. The highlight rides inside the row,
    // so the two can never part company. Read in layout, so a fold re-places
    // this box each frame and recomposes nothing.
    Box(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
            val f = fold().coerceIn(0f, 1f)
            val slot = TAB_SLOT.roundToPx()
            val step = slot + TAB_GAP.roundToPx()
            val shown = (placeable.width + (slot - placeable.width) * f).roundToInt()
            layout(shown, placeable.height) {
                placeable.place((-selectedIndex * step * f).roundToInt(), 0)
            }
        },
    ) {
        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .graphicsLayer {
                        translationX = animatedPillOffset
                        // Around its own centre, so the indicator draws out
                        // both ways rather than growing a tail off one edge —
                        // a leading edge that ran ahead of the glyph it is
                        // meant to be behind would read as two things moving,
                        // not one thing stretching.
                        scaleX = 1f + lag * STRETCH
                        scaleY = 1f - lag * STRETCH * SQUASH
                    }
                    .clip(pillShape)
                    // The selected tab sits in a circle of TuneTable's accent.
                    .background(Accent.brush),
            )
        }

        Row(
            modifier = Modifier
                .onSizeChanged { rowSize = it }
                .pointerInput(collapsed) {
                    // Nothing to drag between while folded.
                    if (collapsed) return@pointerInput
                    // Every drag measures from the tab selected when it starts.
                    // A stale index here measured them all from the tab the bar
                    // first opened on: a drag right of it still landed, while
                    // one to the left clamped back onto it and read as no change.
                    var totalDrag = 0f
                    var dragStartIndex = currentSelectedIndex
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            dragStartIndex = currentSelectedIndex
                        },
                        onDragCancel = { dragOffset = 0f },
                        onDragEnd = {
                            val step = currentTabStepPx
                            if (step > 0f) {
                                val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                    .coerceIn(0f, currentLastIndex.toFloat())
                                val newIndex = absoluteIndexFloat.roundToInt()
                                if (newIndex != currentSelectedIndex) {
                                    currentOnTabSelected(newIndex)
                                }
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            val step = currentTabStepPx
                            if (step <= 0f) return@detectHorizontalDragGestures
                            totalDrag += delta
                            val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                .coerceIn(0f, currentLastIndex.toFloat())
                            dragOffset = absoluteIndexFloat * step - currentSelectedIndex * step

                            val approxTab = absoluteIndexFloat.roundToInt()
                            if (approxTab != lastHapticTab) {
                                haptics.play(Haptic.Tick)
                                lastHapticTab = approxTab
                            }
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(TAB_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A real glass pill samples whatever artwork is behind it, not the
            // theme's surface color, so a fixed onSurfaceVariant gray can lose
            // contrast against it. Glass mode reads luminance off the surface
            // color instead and picks pure black or white, same as the tint
            // Echo's floating nav bar uses for its own liquid glass.
            val glassTint = glassContentColor()
            val adaptiveTint = if (useGlass) glassTint else null
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                    BottomBarItem(
                        tab = tab,
                        selected = selected,
                        glassSpec = glassSpec,
                        // White on the accent circle.
                        selectedTint = Color.White,
                        unselectedTint = adaptiveTint?.copy(alpha = 0.65f),
                        onClick = { if (collapsed) onExpand() else onTabSelected(index) },
                        onActiveTabTap = { if (!collapsed) onActiveTabTap() },
                        modifier = Modifier
                            .size(TAB_SLOT)
                            // The others fade as they fold out of sight.
                            .graphicsLayer { alpha = if (selected) 1f else 1f - fold().coerceIn(0f, 1f) },
                    )
            }
        }
    }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    glassSpec: AnimationSpec<Float>,
    onClick: () -> Unit,
    onActiveTabTap: () -> Unit,
    modifier: Modifier = Modifier,
    /** Overrides the theme's primary/onSurfaceVariant tint — see the glass branch above. */
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    // The same spring the indicator rides, so the glyph arriving and the glass
    // arriving are one movement rather than two that nearly agree.
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = glassSpec,
        label = "tabScale",
    )
    val haptics = rememberHaptics()
    val tint by animateColorAsState(
        targetValue = if (selected) {
            selectedTint ?: MaterialTheme.colorScheme.primary
        } else {
            unselectedTint ?: MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "tabTint",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (selected) {
                    onActiveTabTap()
                } else {
                    haptics.play(Haptic.Select)
                }
                onClick()
            },
    ) {
        // Icons only: the label is still the icon's description, so TalkBack
        // reads each tab's name.
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(TAB_GLYPH)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

/** The tab pill's own height: one tab's circle and the inset round it. */
val FLOATING_TAB_PILL_HEIGHT: Dp get() = TAB_SLOT + PILL_INSET * 2

/** Each tab's round target, which the accent circle fills when it is selected. */
private val TAB_SLOT = 48.dp

/** The gap between two tabs' circles. */
private val TAB_GAP = 6.dp

/** The tab icons inside their circles. */
private val TAB_GLYPH = 24.dp
