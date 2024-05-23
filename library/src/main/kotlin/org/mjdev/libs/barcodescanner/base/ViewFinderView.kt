package org.mjdev.libs.barcodescanner.base

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.mjdev.libs.barcodescanner.drawable.BarcodeDrawable
import com.google.zxing.Result
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Custom overlay, highly manageable
 * That made frame above camera to made design requested
 */
@Suppress("PrivatePropertyName")
class ViewFinderView(context: Context) : View(context) {
    private var SCANNER_ALPHA = arrayOf(0, 64, 128, 192, 255, 192, 128, 64)
    private var POINT_SIZE: Int = 10
    private var ANIMATION_DELAY: Long = 80L

    private var maskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var maskDrawableResId: Int = -1
    private var maskBitmap: Bitmap? = null
    private var framePaint: Paint
    private var path: Path
    private var laserPaint: Paint
    private var frameRect: Rect = Rect(0, 0, 0, 0)
    private var frameCornersSize: Int = 0
    private var frameCornersRadius: Int = 0
    private var frameRatioWidth: Float = 1f
    private var frameRatioHeight: Float = 1f
    private var frameSize: Float = 0.75f
    private var scannerAlpha: Int = 0
    private var laserColor: Int = Color.RED
    private var laserLineHeight: Int = 2
    private var isLaserEnabled: Boolean = true
    private var resultToDraw: Result? = null
    private var resultPadding: Int = 0

    init {
        maskPaint.style = Paint.Style.FILL
        framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        laserPaint = Paint().apply {
            color = laserColor
            style = Paint.Style.FILL
        }
        path = Path().apply {
            fillType = Path.FillType.EVEN_ODD
        }
    }

    override fun onDraw(canvas: Canvas) {
        val width: Float = width.toFloat()
        val height: Float = height.toFloat()
        val top: Float = frameRect.top.toFloat()
        val left: Float = frameRect.left.toFloat()
        val right: Float = frameRect.right.toFloat()
        val bottom: Float = frameRect.bottom.toFloat()
        val frameCornersSize: Float = frameCornersSize.toFloat()
        val frameCornersRadius: Float = frameCornersRadius.toFloat()
        val path: Path = path
        val normalizedRadius: Float = min(
            frameCornersRadius,
            max(frameCornersSize - 1f, 0f)
        )
        if (maskDrawableResId != -1) {
            canvas.drawBitmap(
                getMaskBitmap(
                    left, top, right, bottom, width, height
                ), 0f, 0f, null
            )
        } else {
            // square mask
            path.reset()
            path.moveTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, top + normalizedRadius)
            path.moveTo(0.0F, 0.0F)
            path.lineTo(width, 0.0F)
            path.lineTo(width, height)
            path.lineTo(0.0F, height)
            path.lineTo(0.0F, 0.0F)
            canvas.drawPath(path, maskPaint)
        }
        // corners
        path.reset()
        path.moveTo(left, top + frameCornersSize)
        path.lineTo(left, top + normalizedRadius)
        path.quadTo(left, top, left + normalizedRadius, top)
        path.lineTo(left + frameCornersSize, top)
        path.moveTo(right - frameCornersSize, top)
        path.lineTo(right - normalizedRadius, top)
        path.quadTo(right, top, right, top + normalizedRadius)
        path.lineTo(right, top + frameCornersSize)
        path.moveTo(right, bottom - frameCornersSize)
        path.lineTo(right, bottom - normalizedRadius)
        path.quadTo(right, bottom, right - normalizedRadius, bottom)
        path.lineTo(right - frameCornersSize, bottom)
        path.moveTo(left + frameCornersSize, bottom)
        path.lineTo(left + normalizedRadius, bottom)
        path.quadTo(left, bottom, left, bottom - normalizedRadius)
        path.lineTo(left, bottom - frameCornersSize)
        canvas.drawPath(path, framePaint)
        // result if exists
        if (resultToDraw != null) {
            drawResultThrough(canvas, resultToDraw)
        }
        // laser if enabled
        if (isLaserEnabled) {
            drawLaser(canvas)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        maskBitmap = null
    }

    private fun getMaskBitmap(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        width: Float,
        height: Float
    ): Bitmap {
        if (maskBitmap == null) {
            ContextCompat.getDrawable(context, maskDrawableResId)?.toBitmap(
                max((right - left).toInt(), 1),
                max((bottom - top).toInt(), 1)
            ).let { maskBmp ->
                Bitmap.createBitmap(
                    max(width.toInt(), 1),
                    max(height.toInt(), 1),
                    Bitmap.Config.ARGB_8888
                ).apply {
                    eraseColor(maskPaint.color)
                }.let { bmp ->
                    Canvas(bmp).apply {
                        if (maskBmp != null) {
                            drawBitmap(maskBmp, left, top, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                            })
                        }
                    }
                    maskBitmap = bmp
                }
            }
        }
        return maskBitmap!!
    }

    private fun drawResultThrough(canvas: Canvas, result: Result?) {
        if (result != null) {
            val framingRect: Rect = getFrameRect()
            BarcodeDrawable(
                format = result.barcodeFormat,
                data = result.text ?: "",
                padding = resultPadding
            ).apply {
                setBounds(
                    framingRect.left,
                    framingRect.top,
                    framingRect.right,
                    framingRect.bottom
                )
            }.let {
                if (maskDrawableResId == -1) it
                else it.maskWith(context, maskDrawableResId)
            }.draw(canvas)
        }
    }

    private fun drawLaser(canvas: Canvas) {
        val framingRect: Rect = getFrameRect()
        laserPaint.alpha = SCANNER_ALPHA[scannerAlpha]
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
        val middle: Int = framingRect.height / 2 + framingRect.top
        val laserL = laserLineHeight.toFloat() / 2
        canvas.drawRect(
            framingRect.left + 2f,
            middle - laserL,
            framingRect.right - laserL,
            middle + 2f,
            laserPaint
        )
        postInvalidateDelayed(
            ANIMATION_DELAY,
            framingRect.left - POINT_SIZE,
            framingRect.top - POINT_SIZE,
            framingRect.right + POINT_SIZE,
            framingRect.bottom + POINT_SIZE
        )
    }

    @Override
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        invalidateFrameRect(right - left, bottom - top)
    }

    /**
     * Return rect frame used to scan
     */
    fun getFrameRect(): Rect {
        return frameRect
    }

    /**
     * Set aspect ratio
     */
    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        frameRatioWidth = ratioWidth
        frameRatioHeight = ratioHeight
        invalidateFrameRect()
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Get aspect width
     */
    @FloatRange(from = 0.0, fromInclusive = false)
    fun getFrameAspectRatioWidth(): Float {
        return frameRatioWidth
    }

    /**
     * Set aspect width
     */
    fun setFrameAspectRatioWidth(@FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float) {
        frameRatioWidth = ratioWidth
        invalidateFrameRect()
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Get aspect height
     */
    @FloatRange(from = 0.0, fromInclusive = false)
    fun getFrameAspectRatioHeight(): Float {
        return frameRatioHeight
    }

    /**
     * Set aspect height
     */
    fun setFrameAspectRatioHeight(
        @FloatRange(
            from = 0.0,
            fromInclusive = false
        ) ratioHeight: Float
    ) {
        frameRatioHeight = ratioHeight
        invalidateFrameRect()
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Get mask color
     * color that is around a hole to scan
     */
    @ColorInt
    fun getMaskColor(): Int {
        return maskPaint.color
    }

    /**
     * Set mask color
     * color about hole that is around scan area
     */
    fun setMaskColor(@ColorInt color: Int) {
        maskPaint.color = color
        maskBitmap = null
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Return drawable used to mask hole for scanning
     */
    @DrawableRes
    fun getMaskDrawable(): Int {
        return maskDrawableResId
    }

    /**
     * Set mask drawable for hole used for scanning
     */
    fun setMaskDrawable(@DrawableRes drawable: Int) {
        maskDrawableResId = drawable
        maskBitmap = null
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Set laser color
     * laser is line in the middle on scan area
     */
    @Suppress("unused")
    fun setLaserColor(@ColorInt laserColor: Int) {
        laserPaint.color = laserColor
    }

    /**
     * Get laser color
     * laser is line in the middle on scan area
     */
    @Suppress("unused")
    @ColorInt
    fun getLaserColor(): Int {
        return laserPaint.color
    }

    /**
     * Frame color
     * frame is around hole used to scan
     */
    @ColorInt
    fun getFrameColor(): Int {
        return framePaint.color
    }

    /**
     * Frame color
     * frame is around hole used to scan
     */
    fun setFrameColor(@ColorInt color: Int) {
        framePaint.color = color
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Frame thickness
     * frame is around hole used to scan
     */
    @Px
    fun getFrameThickness(): Int {
        return framePaint.strokeWidth.toInt()
    }

    /**
     * Frame thickness
     * frame is around hole used to scan
     */
    fun setFrameThickness(@Px thickness: Int) {
        framePaint.strokeWidth = thickness.toFloat()
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Frame corners size
     * frame is around hole used to scan
     */
    @Px
    fun getFrameCornersSize(): Int {
        return frameCornersSize
    }

    /**
     * Frame corners size
     * frame is around hole used to scan
     */
    fun setFrameCornersSize(@Px size: Int) {
        frameCornersSize = size
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Frame corners radius
     * frame is around hole used to scan
     */
    @Px
    fun getFrameCornersRadius(): Int {
        return frameCornersRadius
    }

    /**
     * Frame corners radius
     * frame is around hole used to scan
     */
    fun setFrameCornersRadius(@Px radius: Int) {
        frameCornersRadius = radius
        if (isLaidOut) {
            invalidate()
        }
    }

    /**
     * Frame size
     * frame is around hole used to scan
     */
    @FloatRange(from = 0.1, to = 1.0)
    fun getFrameSize(): Float {
        return frameSize
    }

    /**
     * laser thickness
     * laser is line in the middle of scanning area
     */
    fun setLaserSize(size: Int) {
        laserLineHeight = size
    }

    /**
     * laser
     * laser is line in the middle of scanning area
     */
    fun setLaserEnabled(enabled: Boolean) {
        isLaserEnabled = enabled
    }

    /**
     * Custom image drawn to scan area as a result if set
     */
    fun drawResult(result: Result?, padding: Int) {
        resultToDraw = result
        resultPadding = padding
    }

    /**
     * Clear scan area
     */
    fun clearResult() {
        resultToDraw = null
    }

    /**
     * Frame size
     * frame is around hole used to scan
     */
    fun setFrameSize(@FloatRange(from = 0.1, to = 1.0) size: Float) {
        frameSize = size
        invalidateFrameRect()
        if (isLaidOut) {
            invalidate()
        }
    }

    private fun invalidateFrameRect() {
        invalidateFrameRect(width, height)
    }

    private fun invalidateFrameRect(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val viewAR: Float = width.toFloat() / height.toFloat()
            val frameAR: Float = frameRatioWidth / frameRatioHeight
            val frameWidth: Int
            val frameHeight: Int
            if (viewAR <= frameAR) {
                frameWidth = (width * frameSize).roundToInt()
                frameHeight = (frameWidth / frameAR).roundToInt()
            } else {
                frameHeight = (height * frameSize).roundToInt()
                frameWidth = (frameHeight * frameAR).roundToInt()
            }
            val frameLeft: Int = (width - frameWidth) / 2
            val frameTop: Int = (height - frameHeight) / 2
            frameRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
        }
    }
}
