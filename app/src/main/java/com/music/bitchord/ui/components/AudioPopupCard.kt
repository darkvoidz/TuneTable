package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import com.music.bitchord.ui.theme.Accent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.utils.containSheetGestures
import com.music.bitchord.ui.player.LocalPlayerFrost
import dev.chrisbanes.haze.HazeState

private val POPUP_CARD_SHAPE = RoundedCornerShape(ALERT_CORNER)
private val POPUP_SCRIM_COLOR = Color.Black.copy(alpha = 0.4f)
private val POPUP_WIDTH = 320.dp
private val POPUP_CONTENT_MAX_HEIGHT = 420.dp

/**
 * The frosted card the output drawer's popups open into — the one
 * [AudioPipelineDialog] was drawn in, lifted out so the Cast picker is the same
 * object rather than a second one made to look like it.
 *
 * A scrim that dismisses, a card that swallows the tap so touching it does not,
 * a centred title and subtitle, a scrolling body, and a full-width closing
 * action. Fixed to the player's own dark palette rather than the theme-adaptive
 * one the settings dialogs use, since everything on the player is white alphas
 * whatever the app's light/dark theme is.
 *
 * [content] is the scrolling body, handed a [BoxScope] so a body can lay
 * something behind its rows — the pipeline draws its signal line there.
 */
@Composable
internal fun AudioPopupCard(
    hazeState: HazeState,
    title: String,
    subtitle: String,
    doneLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** A glyph for the round gradient badge above the title, or null for none. */
    badge: ImageVector? = null,
    /** Anything to stand above the Done button, such as a second action. */
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val frost = LocalPlayerFrost.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(POPUP_SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(POPUP_WIDTH)
                .clip(POPUP_CARD_SHAPE)
                .then(
                    // The ⋯ menu's frost — see [PlayerDrawer], whose drawers
                    // these popups open from.
                    when {
                        reduceDynamicBlur -> Modifier.background(MenuFrostDark)
                        frost != null -> with(frost) {
                            Modifier.frostPanel(POPUP_CARD_SHAPE, MenuFrostDark.copy(alpha = MENU_FROST_ALPHA))
                        }
                        else -> Modifier.liveMenuFrost(hazeState, MenuFrostDark)
                    }
                )
                // A faint wash of the accent, as the drawers and menus have.
                .background(
                    Brush.linearGradient(
                        listOf(Accent.Start.copy(alpha = POPUP_WASH), Accent.End.copy(alpha = POPUP_WASH * 0.8f)),
                    ),
                )
                // Swallows the tap before it reaches the scrim behind, so
                // touching the card itself never dismisses it.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Accent.brush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.W700,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .heightIn(max = POPUP_CONTENT_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )

            // Full width: the default rule is indented to clear the pipeline's
            // icon lane, which left this one sitting right of centre.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                footer()
                // Done as an accent pill, rather than a row of plain text.
                PopupPillButton(label = doneLabel, onClick = onDismiss, primary = true)
            }
        }
    }
}

/** A full-width pill: the accent for the main action, a quiet tint for the rest. */
@Composable
internal fun PopupPillButton(label: String, onClick: () -> Unit, primary: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (primary) Accent.Solid else Color.White.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.W700),
            color = if (primary) Accent.OnSolid else Color.White,
        )
    }
}

/** How much of the accent washes over a popup card. */
private const val POPUP_WASH = 0.08f
