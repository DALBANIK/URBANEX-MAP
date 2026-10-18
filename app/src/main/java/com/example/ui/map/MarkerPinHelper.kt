package com.example.ui.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.MarkerCategory
import com.example.data.model.SkinCatalog

object MarkerPinHelper {

    private val pinCache = mutableMapOf<String, Drawable>()
    private val avatarCache = mutableMapOf<String, Drawable>()

    fun getPinDrawable(context: Context, category: MarkerCategory, markerSkin: String = "default"): Drawable {
        val cacheKey = "${category.key}_$markerSkin"
        pinCache[cacheKey]?.let { return it }

        val density = context.resources.displayMetrics.density
        val width = (42 * density).toInt()
        val height = (54 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val skinItem = SkinCatalog.getMarkerSkin(markerSkin)
        val pinColor = if (markerSkin != "default") skinItem.pinColor.toArgb() else category.color.toArgb()

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0)
        }
        canvas.drawOval(
            RectF(width * 0.2f, height * 0.84f, width * 0.8f, height * 0.98f),
            shadowPaint
        )

        // Teardrop pin path
        val path = Path()
        val radius = width * 0.42f
        val centerX = width / 2f
        val centerY = radius + 3 * density

        path.moveTo(centerX, height * 0.88f)
        path.cubicTo(
            centerX - radius * 0.7f, height * 0.62f,
            centerX - radius, centerY + radius * 0.6f,
            centerX - radius, centerY
        )
        path.arcTo(
            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            180f,
            180f,
            false
        )
        path.cubicTo(
            centerX + radius, centerY + radius * 0.6f,
            centerX + radius * 0.7f, height * 0.62f,
            centerX, height * 0.88f
        )
        path.close()

        // Draw pin body
        paint.color = pinColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // Pin outline border
        paint.color = if (markerSkin == "gold_crown") Color.parseColor("#FFFBEB") else Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * density
        canvas.drawPath(path, paint)

        // Inner white circle
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius * 0.52f, paint)

        // Draw skin emoji or category dot
        if (markerSkin != "default" && skinItem.emoji.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 15f * density
                textAlign = Paint.Align.CENTER
            }
            // Vertical center font offset
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(skinItem.emoji, centerX, textY, textPaint)
        } else {
            // Inner category dot
            paint.color = pinColor
            canvas.drawCircle(centerX, centerY, radius * 0.28f, paint)
        }

        val drawable = BitmapDrawable(context.resources, bitmap)
        pinCache[cacheKey] = drawable
        return drawable
    }

    fun getAvatarDrawable(context: Context, avatarSkinId: String): Drawable {
        avatarCache[avatarSkinId]?.let { return it }

        val density = context.resources.displayMetrics.density
        val size = (46 * density).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val radius = size * 0.44f

        val skin = SkinCatalog.getAvatar(avatarSkinId)
        val accentColor = skin.accentColor.toArgb()

        // Outer glow/pulse circle
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, size * 0.48f, glowPaint)

        // Main colored circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // White border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3.5f * density
        }
        canvas.drawCircle(center, center, radius, borderPaint)

        // Inner white disc
        val innerDisc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius * 0.72f, innerDisc)

        // Emoji
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f * density
            textAlign = Paint.Align.CENTER
        }
        val textY = center - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(skin.emoji, center, textY, textPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        avatarCache[avatarSkinId] = drawable
        return drawable
    }
}
