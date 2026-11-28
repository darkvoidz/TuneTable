package com.music.bitchord.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.LruCache
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.NOTIFICATION_ART_PX
import com.music.bitchord.data.model.PLAYER_ART_MEMORY_CACHE_KEY_EXTRA
import com.music.bitchord.data.model.PLAYER_ART_PX
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.artworkAt
import java.util.concurrent.ConcurrentHashMap

/**
 * The cover a widget shows: the track's artwork, cropped from its centre to the
 * size the layout gives it and rounded (or cut to a circle) to match.
 *
 * Done here rather than in the layout because a widget's [android.widget.ImageView]
 * cannot clip to a shape on every launcher; a bitmap that is already the shape
 * looks the same everywhere.
 */
internal object MediaWidgetArt {

    /**
     * The cover for [artworkUrl] at [widthPx] × [heightPx], with corners of
     * [radiusPx] (half the shorter side makes a circle). Null when there is no
     * artwork, or it could not be had; the layout's placeholder shows instead.
     */
    suspend fun cover(context: Context, artworkUrl: String?, widthPx: Int, heightPx: Int, radiusPx: Float): Bitmap? {
        if (artworkUrl.isNullOrBlank() || widthPx <= 0 || heightPx <= 0) return null
        val key = "$artworkUrl|$widthPx|$heightPx|$radiusPx"
        covers.get(key)?.let { return it }
        if (isCoolingOff(artworkUrl)) return null
        val source = loadArtwork(context, artworkUrl, maxOf(widthPx, heightPx))
        if (source == null) {
            failures[artworkUrl] = SystemClock.elapsedRealtime()
            return null
        }
        return shaped(source, widthPx, heightPx, radiusPx).also { covers.put(key, it) }
    }

    /** A cover already drawn at this size, without waiting on anything. */
    fun peek(artworkUrl: String?, widthPx: Int, heightPx: Int, radiusPx: Float): Bitmap? =
        artworkUrl?.let { covers.get("$it|$widthPx|$heightPx|$radiusPx") }

    /** Drops everything remembered — the last widget has just been removed. */
    fun clear() {
        covers.evictAll()
        failures.clear()
    }

    /**
     * The artwork for [url], at the smallest size in the app's own artwork ladder
     * that covers [longestSidePx], falling back to the size the media session
     * asked for — so it is usually already on disk rather than a fresh download.
     */
    internal suspend fun loadArtwork(context: Context, url: String?, longestSidePx: Int): Bitmap? {
        if (url.isNullOrBlank()) return null
        val px = artPxFor(longestSidePx)
        val fallbackPx = NOTIFICATION_ART_PX.takeIf { it != px }
        return load(context, url, px) ?: fallbackPx?.let { load(context, url, it) }
    }

    private suspend fun load(context: Context, url: String, px: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url.artworkAt(px) ?: url)
            .size(px)
            .apply {
                if (px == PLAYER_ART_PX) {
                    memoryCacheKeyExtra(PLAYER_ART_MEMORY_CACHE_KEY_EXTRA, px.toString())
                }
            }
            .allowHardware(false) // drawn into a software canvas below
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    private fun artPxFor(longestSidePx: Int): Int = when {
        longestSidePx <= ROW_ART_PX -> ROW_ART_PX
        longestSidePx <= CARD_ART_PX -> CARD_ART_PX
        longestSidePx <= NOTIFICATION_ART_PX -> NOTIFICATION_ART_PX
        longestSidePx <= HEADER_ART_PX -> HEADER_ART_PX
        else -> PLAYER_ART_PX
    }

    /** [src] centre-cropped into [w] × [h] and clipped to a rounded rectangle. */
    private fun shaped(src: Bitmap, w: Int, h: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
        return out
    }

    /** A cover that just failed isn't asked for again on every play and pause. */
    private fun isCoolingOff(url: String): Boolean {
        val failedAt = failures[url] ?: return false
        val since = SystemClock.elapsedRealtime() - failedAt
        if (since in 0 until FAILURE_COOLDOWN_MS) return true
        failures.remove(url)
        return false
    }

    /** Sized in bitmaps, not bytes: a handful of covers across five widgets. */
    private val covers = LruCache<String, Bitmap>(12)
    private val failures = ConcurrentHashMap<String, Long>()
    private const val FAILURE_COOLDOWN_MS = 30_000L
}
