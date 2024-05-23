package org.mjdev.libs.barcodescanner.drawable

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import kotlin.math.max

/**
 * Custom drawable showing barcode
 * @param format qr code format (zxing)
 * @param data string with data to encode
 * @param backgroundColor background color
 */
@Suppress("MemberVisibilityCanBePrivate")
class BarcodeDrawable(
    val format: BarcodeFormat,
    val data: String,
    val backgroundColor: Int = Color.WHITE,
    val padding: Int = 64
) : Drawable() {
    private val paint = Paint()

    init {
        paint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        val bmp: Bitmap? = try {
            generateBarcode(format, data, bounds.width(), bounds.height(), padding)
        } catch (e: Exception) {
            null
        }
        canvas.drawRect(bounds, paint)
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, bounds, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.OPAQUE", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    @Throws(WriterException::class)
    private fun generateBarcode(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        padding: Int = 0
    ): Bitmap = try {
        MultiFormatWriter().encode(
            data,
            format,
            (width - (padding * 2)),
            (height - (padding * 2))
        ).let { bmx ->
            Bitmap.createBitmap(
                (bmx.width + (padding * 2)),
                (bmx.height + (padding * 2)),
                Bitmap.Config.RGB_565
            ).apply {
                eraseColor(backgroundColor)
                for (x in 0 until bmx.width) {
                    for (y in 0 until bmx.height) {
                        setPixel(
                            (x + padding),
                            (y + padding),
                            if (bmx[x, y]) Color.BLACK else Color.WHITE
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        Bitmap.createBitmap(
            max(width, 1),
            max(height, 1),
            Bitmap.Config.RGB_565
        )
    }

    /**
     * Mask barcode drawable with another drawable
     * Used for example to made circled barcode scanner result image
     * @param context context
     * @param drawableResId id of mask drawable, circle shape for example
     */
    fun maskWith(context: Context, @DrawableRes drawableResId: Int): Drawable {
        if (drawableResId != -1) {
            val rect = bounds
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            val source = generateBarcode(
                format,
                data,
                max(rect.width(), 1),
                max(rect.height(), 1),
                padding
            )
            val mask = ContextCompat.getDrawable(context, drawableResId)!!.toBitmap(
                max(rect.width(), 1),
                max(rect.height(), 1)
            )
            val result = Bitmap.createBitmap(
                max(rect.width(), 1),
                max(rect.height(), 1),
                Bitmap.Config.ARGB_8888
            )
            Canvas(result).apply {
                drawBitmap(source, 0f, 0f, paint)
                drawBitmap(mask, 0f, 0f, maskPaint)
            }
            maskPaint.xfermode = null
            return BitmapDrawable(context.resources, result).apply {
                bounds = rect
            }
        } else {
            return this
        }
    }
}
