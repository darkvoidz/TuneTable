package com.music.bitchord.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import com.music.bitchord.playback.PlayerDeepLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * The home-screen widgets: five sizes of one now-playing card, each drawn in the
 * app's Style (Subtle, Deep, Glow, Duotone, Neon) and accent colour.
 *
 *  - 2×1 [MediaWidgetMini]: cover, title, play.
 *  - 4×1 [MediaWidgetPill]: round cover, title and artist, previous, play, next.
 *  - 2×2 [MediaWidgetSquare]: cover and play, then title and artist.
 *  - 4×2 [MediaWidgetWide]: a large cover, the track, like and the transport.
 *  - 4×4 [MediaWidgetLarge]: a big cover, the track, shuffle, transport, like.
 *
 * The card, the play button's disc and the cover are bitmaps drawn here
 * ([MediaWidgetStyle], [MediaWidgetArt]) because a widget's views can't draw
 * gradients, glows or rings in a colour chosen at runtime. The glyphs are plain
 * white drawables tinted per style.
 *
 * The three older providers keep their class names so widgets already on a home
 * screen carry over into the new design rather than disappearing.
 */
abstract class MediaWidget : AppWidgetProvider() {

    internal abstract val kind: Kind

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAsync(context, ids)
    }

    /** A resize changes the card's bitmap, so it is a redraw. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) {
        renderAsync(context, intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        for (id in ids) lastCover.remove(id)
    }

    override fun onDisabled(context: Context) {
        MediaWidgetArt.clear()
    }

    /** Off the broadcast thread, holding the broadcast open while artwork loads. */
    private fun renderAsync(context: Context, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        val kind = kind
        scope.launch {
            try {
                render(app, kind, ids, SystemClock.elapsedRealtime() + BROADCAST_BUDGET_MS)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    /**
     * What differs between the five sizes: the provider, its layout, the size to
     * assume before the launcher says, and how much of it the cover takes.
     */
    internal enum class Kind(
        val provider: Class<out MediaWidget>,
        val layout: Int,
        val fallbackWidthDp: Int,
        val fallbackHeightDp: Int,
        val playDp: Int,
        /** The card is a capsule rather than a rounded rectangle. */
        val capsule: Boolean,
    ) {
        MINI(MediaWidgetMini::class.java, R.layout.widget_styled_mini, 110, 40, 34, capsule = true),
        PILL(MediaWidgetPill::class.java, R.layout.widget_styled_pill, 250, 40, 38, capsule = true),
        SQUARE(MediaWidgetSquare::class.java, R.layout.widget_styled_square, 110, 110, 40, capsule = false),
        WIDE(MediaWidgetWide::class.java, R.layout.widget_styled_wide, 250, 110, 52, capsule = false),
        LARGE(MediaWidgetLarge::class.java, R.layout.widget_styled_large, 250, 250, 72, capsule = false),
        ;

        /** The cover's size in dp, from the widget's own measured size. */
        fun coverDp(widthDp: Int, heightDp: Int): Pair<Int, Int> = when (this) {
            MINI, PILL -> 40 to 40
            SQUARE -> 56 to 56
            // Fills the card's height inside its 12dp padding, square.
            WIDE -> (heightDp - 24).coerceAtLeast(40).let { it to it }
            // Whatever the 16dp padding, the track (about 40dp) and the
            // controls row (72dp), with their 12dp gaps, leave.
            LARGE -> (widthDp - 32).coerceAtLeast(40) to (heightDp - 32 - 40 - 12 - 72 - 12).coerceAtLeast(40)
        }

        fun coverRadiusDp(coverW: Int, coverH: Int): Float = when (this) {
            PILL -> minOf(coverW, coverH) / 2f
            MINI -> 10f
            SQUARE -> 12f
            WIDE -> 14f
            LARGE -> 18f
        }
    }

    companion object {

        /**
         * Redraws every placed widget. Called by the playback service on every
         * play, pause and track change, and by the app when the Style or accent
         * colour changes — so it only hands off on the calling thread.
         */
        fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch {
                val manager = runCatching { AppWidgetManager.getInstance(app) }.getOrNull() ?: return@launch
                for (kind in Kind.entries) {
                    val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, kind.provider)) }.getOrNull()
                    if (ids == null || ids.isEmpty()) continue
                    render(app, kind, ids, deadline = null)
                }
            }
        }

        private suspend fun render(context: Context, kind: Kind, ids: IntArray, deadline: Long?) {
            val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val snapshot = MediaWidgetSnapshot.load(context)
            val palette = MediaWidgetStyle.current(context)
            for (id in ids) {
                val budget = when (deadline) {
                    null -> RENDER_TIMEOUT_MS
                    else -> minOf(RENDER_TIMEOUT_MS, deadline - SystemClock.elapsedRealtime())
                }
                if (budget <= 0L) return
                withTimeoutOrNull(budget) { render(context, manager, kind, id, snapshot, palette) }
            }
        }

        private suspend fun render(
            context: Context,
            manager: AppWidgetManager,
            kind: Kind,
            id: Int,
            snapshot: MediaWidgetSnapshot,
            palette: MediaWidgetStyle.Palette,
        ) {
            val (widthDp, heightDp) = measure(context, manager, id, kind)
            val density = context.resources.displayMetrics.density
            val (coverW, coverH) = kind.coverDp(widthDp, heightDp)
            val coverWpx = (coverW * density).roundToInt()
            val coverHpx = (coverH * density).roundToInt()
            val coverRadius = kind.coverRadiusDp(coverW, coverH) * density

            // The card and the play disc are cheap; draw them now so the widget
            // answers a tap at once, then fill the cover in once it has loaded.
            val cornerDp = context.resources.getDimension(R.dimen.widget_corner_radius) / density
            val card = MediaWidgetStyle.card(palette, widthDp, heightDp, density, cornerDp, kind.capsule)
            val disc = MediaWidgetStyle.playDisc(palette, kind.playDp, density)
            val cached = MediaWidgetArt.peek(snapshot.artworkUrl, coverWpx, coverHpx, coverRadius)
            if (cached == null) {
                // A RemoteViews replaces the whole tree, so a push without the
                // cover would clear it; hold the last one until the new arrives.
                manager.push(id, views(context, kind, snapshot, palette, card, disc, lastCover[id]))
            }
            val cover = cached
                ?: MediaWidgetArt.cover(context, snapshot.artworkUrl, coverWpx, coverHpx, coverRadius)
            if (cover != null) lastCover[id] = cover else lastCover.remove(id)
            manager.push(id, views(context, kind, snapshot, palette, card, disc, cover))
        }

        // ---- views ----

        private fun views(
            context: Context,
            kind: Kind,
            snapshot: MediaWidgetSnapshot,
            palette: MediaWidgetStyle.Palette,
            card: Bitmap,
            disc: Bitmap,
            cover: Bitmap?,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, kind.layout)
            views.setImageViewBitmap(R.id.widget_bg, card)
            // The layout's own background is only for the picker and the first
            // frame; the card bitmap carries the real one.
            views.setInt(R.id.widget_root, "setBackgroundResource", 0)
            views.setImageViewBitmap(R.id.widget_toggle_bg, disc)
            if (cover != null) {
                views.setImageViewBitmap(R.id.widget_art, cover)
                views.setInt(R.id.widget_art, "setBackgroundResource", 0)
            }

            views.setTextViewText(
                R.id.widget_title,
                if (snapshot.hasTrack) snapshot.title else context.getString(R.string.widget_nothing_played),
            )
            views.setTextColor(R.id.widget_title, palette.title)
            if (kind != Kind.MINI) {
                views.setTextViewText(R.id.widget_artist, snapshot.artist)
                views.setTextColor(R.id.widget_artist, palette.subtitle)
                views.setViewVisibility(R.id.widget_artist, if (snapshot.artist.isBlank()) View.GONE else View.VISIBLE)
            }

            val line = palette.lineGlyphs
            views.setImageViewResource(
                R.id.widget_toggle,
                when {
                    snapshot.isPlaying && line -> R.drawable.ic_widget_pause_line
                    snapshot.isPlaying -> R.drawable.ic_widget_pause
                    line -> R.drawable.ic_widget_play_line
                    else -> R.drawable.ic_widget_play
                },
            )
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(if (snapshot.isPlaying) R.string.widget_pause else R.string.widget_play),
            )
            views.tint(R.id.widget_toggle, palette.playGlyph)

            val open = openPlayer(context)
            views.setOnClickPendingIntent(R.id.widget_root, open)

            val hasTransport = kind != Kind.MINI && kind != Kind.SQUARE
            if (hasTransport) {
                views.setImageViewResource(
                    R.id.widget_previous,
                    if (line) R.drawable.ic_widget_previous_line else R.drawable.ic_widget_previous,
                )
                views.setImageViewResource(R.id.widget_next, if (line) R.drawable.ic_widget_next_line else R.drawable.ic_widget_next)
                views.tint(R.id.widget_previous, palette.icon)
                views.tint(R.id.widget_next, palette.icon)
            }
            val hasLike = kind == Kind.WIDE || kind == Kind.LARGE
            if (hasLike) {
                views.setImageViewResource(
                    R.id.widget_like,
                    if (snapshot.isLiked) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart,
                )
                views.tint(R.id.widget_like, if (snapshot.isLiked) palette.active else palette.icon)
            }
            val hasShuffle = kind == Kind.LARGE
            if (hasShuffle) {
                views.tint(R.id.widget_shuffle, if (snapshot.shuffleEnabled) palette.active else palette.icon)
            }

            if (snapshot.hasTrack) {
                views.setImageAlpha(R.id.widget_toggle, ALPHA_ENABLED)
                views.setOnClickPendingIntent(R.id.widget_toggle, action(context, MediaWidgetActions.ACTION_TOGGLE))
                if (hasTransport) {
                    views.setImageAlpha(R.id.widget_previous, if (snapshot.hasPrevious) ALPHA_ENABLED else ALPHA_DISABLED)
                    views.setImageAlpha(R.id.widget_next, if (snapshot.hasNext) ALPHA_ENABLED else ALPHA_DISABLED)
                    views.setOnClickPendingIntent(R.id.widget_previous, action(context, MediaWidgetActions.ACTION_PREVIOUS))
                    views.setOnClickPendingIntent(R.id.widget_next, action(context, MediaWidgetActions.ACTION_NEXT))
                }
                if (hasLike) {
                    views.setImageAlpha(R.id.widget_like, if (snapshot.canLike) ALPHA_ENABLED else ALPHA_DISABLED)
                    views.setOnClickPendingIntent(
                        R.id.widget_like,
                        if (snapshot.canLike) action(context, MediaWidgetActions.ACTION_LIKE) else open,
                    )
                }
                if (hasShuffle) {
                    views.setImageAlpha(R.id.widget_shuffle, if (snapshot.shuffleEnabled) ALPHA_ENABLED else ALPHA_IDLE)
                    views.setOnClickPendingIntent(R.id.widget_shuffle, action(context, MediaWidgetActions.ACTION_SHUFFLE))
                }
            } else {
                // Nothing has played yet: every control opens the app instead.
                val buttons = buildList {
                    add(R.id.widget_toggle)
                    if (hasTransport) addAll(listOf(R.id.widget_previous, R.id.widget_next))
                    if (hasLike) add(R.id.widget_like)
                    if (hasShuffle) add(R.id.widget_shuffle)
                }
                for (button in buttons) {
                    views.setImageAlpha(button, ALPHA_DISABLED)
                    views.setOnClickPendingIntent(button, open)
                }
            }
            return views
        }

        private fun action(context: Context, action: String) = MediaWidgetActions.pendingIntent(context, action)

        private fun RemoteViews.tint(viewId: Int, color: Int) = setInt(viewId, "setColorFilter", color)

        private fun RemoteViews.setImageAlpha(viewId: Int, alpha: Int) = setInt(viewId, "setImageAlpha", alpha)

        /**
         * Opens the app on the full player. An explicit component with no action,
         * so the singleTask activity always gets it through onNewIntent.
         */
        private fun openPlayer(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(PlayerDeepLink.EXTRA_OPEN_PLAYER, true)
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_PLAYER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** A host that has gone away is not worth crashing a broadcast over. */
        private fun AppWidgetManager.push(id: Int, views: RemoteViews) {
            runCatching { updateAppWidget(id, views) }
        }

        /**
         * This instance's size in dp, for the orientation in force now: portrait
         * is min-width by max-height, landscape the reverse.
         */
        private fun measure(context: Context, manager: AppWidgetManager, id: Int, kind: Kind): Pair<Int, Int> {
            val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val width = options?.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            )?.takeIf { it > 0 } ?: kind.fallbackWidthDp
            val height = options?.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            )?.takeIf { it > 0 } ?: kind.fallbackHeightDp
            return width to height
        }

        /** Per-process, since a provider instance lives only for one broadcast. */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** The cover each widget is showing, kept for the push before a new one loads. */
        private val lastCover = ConcurrentHashMap<Int, Bitmap>()

        private const val RENDER_TIMEOUT_MS = 8_000L
        private const val BROADCAST_BUDGET_MS = 8_000L

        private const val ALPHA_ENABLED = 255
        private const val ALPHA_IDLE = 150
        private const val ALPHA_DISABLED = 90

        private const val REQUEST_OPEN_PLAYER = 0
    }
}

/** 2×1. See `res/xml/widget_media_mini.xml`. */
class MediaWidgetMini : MediaWidget() {
    override val kind get() = Kind.MINI
}

/** 4×1. See `res/xml/widget_media_pill.xml`. */
class MediaWidgetPill : MediaWidget() {
    override val kind get() = Kind.PILL
}

/** 2×2. See `res/xml/widget_media_square.xml`. */
class MediaWidgetSquare : MediaWidget() {
    override val kind get() = Kind.SQUARE
}

/** 4×2. See `res/xml/widget_media_wide.xml`. */
class MediaWidgetWide : MediaWidget() {
    override val kind get() = Kind.WIDE
}

/** 4×4. See `res/xml/widget_media_large.xml`. */
class MediaWidgetLarge : MediaWidget() {
    override val kind get() = Kind.LARGE
}
