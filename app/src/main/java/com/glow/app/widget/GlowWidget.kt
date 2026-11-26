package com.glow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import com.glow.app.MainActivity
import com.glow.app.R
import com.glow.app.media.GlowMediaSessionService
import com.glow.app.media.PlayerState
import com.glow.app.media.displayTitle
import kotlin.math.min
import com.glow.app.GlowStrings

/** ويدجت الشاشة الرئيسية: الغلاف والعنوان والفنان وأزرار السابق/تشغيل/التالي. */
class GlowWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetManager.updateAppWidget(appWidgetIds, GlowWidget.build(context))
    }
}

object GlowWidget {

    private class Snapshot(val title: String, val artist: String, val playing: Boolean, val art: Bitmap?)

    // آخر حالة عُرضت، ليُرسم بها الويدجت عند إضافته أو تحديثه من النظام.
    @Volatile
    private var last: Snapshot? = null

    private val UNKNOWN_ARTIST: String get() = GlowStrings.get(R.string.artist_unknown)
    private const val ART_PX = 240

    /** يحدّث كل نسخ الويدجت. مرّر state بلا مقطع (أو null) لعرض حالة السكون. */
    fun update(context: Context, state: PlayerState?, art: Bitmap?) {
        val track = state?.current
        last = if (state == null || track == null) {
            null
        } else {
            Snapshot(track.displayTitle, track.artist.takeIf { it != UNKNOWN_ARTIST }.orEmpty(), state.isPlaying, art)
        }
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, GlowWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(context))
    }

    internal fun build(context: Context): RemoteViews {
        val snap = last
        val views = RemoteViews(context.packageName, R.layout.glow_widget)
        views.setImageViewBitmap(R.id.widget_art, roundedArt(snap?.art))
        if (snap == null) {
            views.setTextViewText(R.id.widget_title, "Glow")
            views.setTextViewText(R.id.widget_artist, GlowStrings.get(R.string.tap_to_open))
            views.setViewVisibility(R.id.widget_controls, View.GONE)
        } else {
            views.setTextViewText(R.id.widget_title, snap.title)
            views.setTextViewText(R.id.widget_artist, snap.artist)
            views.setViewVisibility(R.id.widget_controls, View.VISIBLE)
            views.setImageViewResource(
                R.id.widget_play,
                if (snap.playing) R.drawable.ic_notif_pause else R.drawable.ic_notif_play,
            )
            views.setOnClickPendingIntent(R.id.widget_prev, servicePending(context, GlowMediaSessionService.ACTION_PREVIOUS, 11))
            views.setOnClickPendingIntent(R.id.widget_play, servicePending(context, GlowMediaSessionService.ACTION_TOGGLE, 12))
            views.setOnClickPendingIntent(R.id.widget_next, servicePending(context, GlowMediaSessionService.ACTION_NEXT, 13))
        }
        views.setOnClickPendingIntent(R.id.widget_root, openAppPending(context))
        return views
    }

    private fun servicePending(context: Context, action: String, code: Int): PendingIntent {
        val intent = Intent(context, GlowMediaSessionService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            context,
            code,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openAppPending(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** غلاف مربع بزوايا مدوّرة (RemoteViews لا يقصّ الزوايا بنفسه)، أو تدرّج الهوية عند غيابه. */
    private fun roundedArt(src: Bitmap?): Bitmap {
        val size = ART_PX
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (src == null) {
            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                0xFF160078.toInt(), 0xFFF042FF.toInt(), Shader.TileMode.CLAMP,
            )
        } else {
            val side = min(src.width, src.height)
            val scale = size.toFloat() / side
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(-(src.width - side) / 2f * scale, -(src.height - side) / 2f * scale)
            }
            paint.shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        }
        val radius = size * 0.2f
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, paint)
        return out
    }
}
