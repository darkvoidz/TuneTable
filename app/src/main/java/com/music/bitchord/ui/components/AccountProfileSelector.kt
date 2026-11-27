package com.music.bitchord.ui.components

import com.music.bitchord.ui.theme.Accent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import com.music.bitchord.ui.icons.SfIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.YouTubeProfile
import com.music.bitchord.data.settings.AppSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Shared compact selector for every visible YouTube account avatar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    hazeState: HazeState,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** On the card alone, under the dim — where the caller hangs its entrance. */
    cardModifier: Modifier = Modifier,
) {
    var managing by remember { mutableStateOf(false) }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    // The thin material lets too much of the page through behind the text. Dark
    // mode gets a black wash over it; light mode a white one, which keeps the
    // dark text readable over a busy cover instead of muddying it.
    val material = HazeMaterials.thin(surface)
    val style = remember(material, dark) {
        HazeStyle(
            backgroundColor = material.backgroundColor,
            tints = material.tints + HazeTint(
                if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.40f),
            ),
            blurRadius = material.blurRadius,
            noiseFactor = material.noiseFactor,
            fallbackTint = material.fallbackTint,
        )
    }
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, start = 20.dp, end = 16.dp)
                .then(cardModifier)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = style,
                        )
                    },
                )
                // The same hairline the mini player and the nav bar wear.
                .border(
                    0.5.dp,
                    // White on dark like the bars; a light surface has nothing
                    // for a white hairline to show against.
                    if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                    shape,
                )
                .clickable(onClick = {}),
        ) {
            // Room under the last row to match the space between rows, so the
            // list doesn't stop short against the card's edge.
            LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                item { Text(stringResource(R.string.switch_account), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 2.dp)) }
                accounts.forEach { account ->
                item {
                    Text(account.email.ifBlank { account.name.ifBlank { stringResource(R.string.accounts) } },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 2.dp))
                }
                items(account.profiles.size) { index ->
                    val profile = account.profiles[index]
                    ProfileRow(profile, account.accountId == activeAccountId && profile.profileId == activeProfileId,
                        managing, { onSelect(account, profile); onDismiss() }, { onRemoveAccount(account) })
                }
                }
                item { SelectorAction(SfIcons.Add, stringResource(R.string.add_account), onClick = onAddAccount) }
                item { SelectorAction(Icons.Rounded.ManageAccounts, stringResource(R.string.manage_accounts)) { managing = !managing } }
                item { SelectorAction(SfIcons.Settings, stringResource(R.string.settings), onClick = onOpenSettings) }
            }
        }
    }
}

@Composable private fun ProfileRow(profile: YouTubeProfile, selected: Boolean, managing: Boolean,
    onClick: () -> Unit, onRemove: () -> Unit) {
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)
    Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).clickable(role = Role.RadioButton, onClick = if (managing) onRemove else onClick)
        .semantics { contentDescription = description }
        .padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (profile.avatar != null) AsyncImage(profile.avatar, null, Modifier.size(PROFILE_AVATAR).clip(MaterialTheme.shapes.extraLarge))
        else Icon(SfIcons.AccountPlaceholder, null, Modifier.size(PROFILE_AVATAR), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(profile.handle.ifBlank { stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal) },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected && !managing) Icon(Icons.Rounded.Check, stringResource(R.string.selected_account),
            Modifier.size(20.dp), tint = Accent.Solid)
        if (managing) Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error)
    }
}

@Composable private fun SelectorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        // In the avatars' column, so the labels line up with the names above.
        Box(Modifier.width(PROFILE_AVATAR), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize)) }
        Spacer(Modifier.width(12.dp)); Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The avatars, and the column the action icons are centred in to line up with them. */
private val PROFILE_AVATAR = 32.dp
