package com.music.bitchord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AccentMode
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.theme.AccentColors
import com.music.bitchord.ui.theme.AccentPresets
import com.music.bitchord.ui.theme.SystemAccents
import com.music.bitchord.ui.theme.toHsv

/**
 * Settings → Appearance → Accent colour: the app icon's gradient, the phone's
 * own system colours, a set of presets, or any colour from the picker. A tap
 * applies at once, so the dialog itself changes colour under the finger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccentColourDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val mode by AppSettings.accentMode.collectAsStateWithLifecycle()
    val chosen by AppSettings.accentColor.collectAsStateWithLifecycle()
    val systemIndex by AppSettings.accentSystemIndex.collectAsStateWithLifecycle()
    val systemColours = remember { SystemAccents.colors(context) }
    val isPreset = mode == AccentMode.COLOR && AccentPresets.any { it.toArgb() == chosen }
    var picking by remember { mutableStateOf(mode == AccentMode.COLOR && !isPreset) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                stringResource(R.string.accent_colour),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Section(stringResource(R.string.accent_app_icon))
            Swatch(
                fill = Brush.linearGradient(listOf(AccentColors.Icon.start, AccentColors.Icon.end)),
                selected = mode == AccentMode.ICON,
                onClick = AppSettings::setAccentIcon,
            )

            if (systemColours.isNotEmpty()) {
                Section(stringResource(R.string.accent_system_colours))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP), verticalArrangement = Arrangement.spacedBy(SWATCH_GAP)) {
                    systemColours.forEachIndexed { index, colour ->
                        Swatch(
                            fill = Brush.linearGradient(listOf(colour, colour)),
                            selected = mode == AccentMode.SYSTEM && systemIndex == index,
                            onClick = { AppSettings.setAccentSystem(index) },
                        )
                    }
                }
            }

            Section(stringResource(R.string.accent_presets))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP), verticalArrangement = Arrangement.spacedBy(SWATCH_GAP)) {
                AccentPresets.forEach { colour ->
                    Swatch(
                        fill = Brush.linearGradient(listOf(colour, colour)),
                        selected = mode == AccentMode.COLOR && chosen == colour.toArgb(),
                        onClick = {
                            picking = false
                            AppSettings.setAccentColor(colour.toArgb())
                        },
                    )
                }
            }

            Section(stringResource(R.string.accent_custom))
            val customSelected = mode == AccentMode.COLOR && !isPreset
            Swatch(
                fill = if (customSelected) {
                    Brush.linearGradient(listOf(Color(chosen), Color(chosen)))
                } else {
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    )
                },
                selected = customSelected,
                glyph = if (customSelected) null else Icons.Rounded.Add,
                onClick = { picking = !picking },
            )
            if (picking) {
                Spacer(Modifier.height(14.dp))
                ColourPicker(
                    initial = Color(if (mode == AccentMode.COLOR) chosen else AccentPresets.first().toArgb()),
                    onPick = { AppSettings.setAccentColor(it.toArgb()) },
                )
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
    )
}

/** One round choice; the chosen one wears a ring and a check. */
@Composable
private fun Swatch(
    fill: Brush,
    selected: Boolean,
    onClick: () -> Unit,
    glyph: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Box(
        modifier = Modifier
            .size(SWATCH)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape).padding(4.dp)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        val icon = if (selected) Icons.Rounded.Check else glyph
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/**
 * A square of saturation (across) and brightness (down) for the current hue,
 * the hue strip under it, and the colour as it stands with its hex code and a
 * button to use it.
 */
@Composable
private fun ColourPicker(initial: Color, onPick: (Color) -> Unit) {
    val start = remember { initial.toHsv() }
    var hue by remember { mutableFloatStateOf(start.first) }
    var saturation by remember { mutableFloatStateOf(start.second.coerceAtLeast(0.05f)) }
    var value by remember { mutableFloatStateOf(start.third.coerceAtLeast(0.2f)) }
    val colour = Color.hsv(hue, saturation, value)
    val shape = RoundedCornerShape(14.dp)

    // Saturation and brightness.
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(shape)
            .pointerInput(Unit) {
                fun set(o: Offset) {
                    saturation = (o.x / size.width).coerceIn(0f, 1f)
                    value = 1f - (o.y / size.height).coerceIn(0f, 1f)
                }
                detectTapGestures { set(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                    value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val at = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(Color.White, radius = 11.dp.toPx(), center = at, style = Stroke(3.dp.toPx()))
        drawCircle(colour, radius = 8.dp.toPx(), center = at)
    }
    Spacer(Modifier.height(14.dp))

    // Hue.
    val hues = remember { (0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) } }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { hue = (it.x / size.width).coerceIn(0f, 1f) * 359.9f }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> hue = (change.position.x / size.width).coerceIn(0f, 1f) * 359.9f }
            },
    ) {
        drawRect(Brush.horizontalGradient(hues))
        val x = hue / 360f * size.width
        drawCircle(Color.White, radius = size.height / 2 - 2.dp.toPx(), center = Offset(x.coerceIn(size.height / 2, size.width - size.height / 2), size.height / 2), style = Stroke(3.dp.toPx()))
    }
    Spacer(Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(colour))
        Spacer(Modifier.width(12.dp))
        Text(
            "#%06X".format(colour.toArgb() and 0xFFFFFF),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colour)
                .clickable { onPick(colour) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                stringResource(R.string.accent_use_this_colour),
                style = MaterialTheme.typography.labelLarge,
                color = if (colour.luminance() > 0.55f) Color.Black else Color.White,
            )
        }
    }
}

private val SWATCH = 44.dp
private val SWATCH_GAP = 12.dp
