package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.theme.Accent
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.time.LocalTime

/** What Home's chips narrow the feed to. */
enum class HomeFilter(val label: StringResource) {
    ALL(Res.string.home_filter_all),
    SONGS(Res.string.home_filter_songs),
    VIDEOS(Res.string.home_filter_videos),
    MIXES(Res.string.home_filter_mixes),
    ;

    /** Whether [item] belongs under this chip. */
    fun keeps(item: ShelfItem): Boolean = when (this) {
        ALL -> true
        SONGS -> item.videoId != null && !item.isVideo
        VIDEOS -> item.isVideo
        // YouTube's own mixes: radio playlists, whose ids start "RD".
        MIXES -> item.browseId?.let { it.startsWith("RD") || it.startsWith("VLRD") } == true
    }
}

/** Every shelf narrowed to [filter], dropping any it leaves empty. */
internal fun List<HomeShelf>.filteredBy(filter: HomeFilter): List<HomeShelf> =
    if (filter == HomeFilter.ALL) {
        this
    } else {
        mapNotNull { shelf ->
            shelf.items.filter(filter::keeps).takeIf { it.isNotEmpty() }?.let { shelf.copy(items = it) }
        }
    }

/**
 * The top of Home: the time of day in the accent over "Welcome back", with the
 * Replay year story's button beside them when there is a year to tell.
 */
@Composable
internal fun HomeGreeting(replayYear: ReplayYearBadge?) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> Res.string.good_morning
            in 12..16 -> Res.string.good_afternoon
            else -> Res.string.good_evening
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(greeting),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Accent.Icon,
            )
            Text(
                stringResource(Res.string.welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (replayYear != null) ReplayYearButton(replayYear, height = 30.dp)
    }
}

/** The chips that narrow the feed — the selected one filled with the accent. */
@Composable
internal fun HomeFilterChips(selected: HomeFilter, onSelect: (HomeFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeFilter.entries.forEach { filter ->
            val on = filter == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (on) Modifier.background(Accent.fillBrush) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant))
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(filter.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) Accent.OnFill else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Your playlists, one tap away, as a single wide button. Left out without its action. */
@Composable
internal fun HomePlaylistsButton(onOpen: (() -> Unit)?) {
    if (onOpen == null) return
    QuickAction(
        Icons.AutoMirrored.Rounded.QueueMusic,
        stringResource(Res.string.your_playlists),
        onOpen,
        Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp)
            .fillMaxWidth(),
    )
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent.Icon, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The album or playlist to jump back into: its name and a picture for it. */
data class JumpBackIn(val title: String, val thumbnailUrl: String?)

/**
 * The album or playlist the latest song was played from, as a card washed in
 * the accent: tap it, or its arrow, to open that page again.
 */
@Composable
internal fun JumpBackInCard(target: JumpBackIn, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .background(
                Brush.linearGradient(
                    listOf(Accent.Start.copy(alpha = 0.40f), Accent.End.copy(alpha = 0.32f)),
                ),
            )
            .clickable(onClick = onOpen)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = target.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.jump_back_in),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(
                target.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Accent.fillBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Accent.OnFill,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Recently played as a two-column grid of tiles: the cover, then the title. */
@Composable
internal fun RecentlyPlayedGrid(
    items: List<ShelfItem>,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    isCurrent: (ShelfItem) -> Boolean,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(
            stringResource(Res.string.recently_played),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 14.dp, bottom = 10.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.take(RECENT_GRID_TILES).chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { item ->
                        RecentTile(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongPress = onItemLongPress?.let { { it(item) } },
                            playing = isCurrent(item) && isPlaying,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentTile(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    playing: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .height(RECENT_TILE_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(RECENT_TILE_HEIGHT)) {
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(RECENT_TILE_HEIGHT)
                    .background(MaterialTheme.colorScheme.surface),
            )
            if (playing) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Text(
            item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

/** How many tiles Recently played shows: three rows of two. */
private const val RECENT_GRID_TILES = 6

private val RECENT_TILE_HEIGHT = 54.dp
