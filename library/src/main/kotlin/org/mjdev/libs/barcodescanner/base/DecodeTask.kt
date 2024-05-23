package org.mjdev.libs.barcodescanner.base

import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result

/**
 * Custom task to decode frame from bytearray
 * which are coming from camera
 */
class DecodeTask(
    private var image: ByteArray,
    private var imageSize: Point,
    private var previewSize: Point,
    private var viewSize: Point,
    private var viewFrameRect: Rect,
    private var orientation: Int,
    private var reverseHorizontal: Boolean
) {
    @SuppressWarnings("SuspiciousNameCombination")
    @Throws(ReaderException::class)
    fun decode(reader: MultiFormatReader): Result? {
        var imageWidth = imageSize.x
        var imageHeight = imageSize.y
        val orientation = orientation
        val image = Utils.rotateYuv(image, imageWidth, imageHeight, orientation)
        if (orientation == 90 || orientation == 270) {
            val width = imageWidth
            imageWidth = imageHeight
            imageHeight = width
        }
        val frameRect = Utils.getImageFrameRect(
            imageWidth,
            imageHeight,
            viewFrameRect,
            previewSize,
            viewSize
        )
        val frameWidth = frameRect.width
        val frameHeight = frameRect.height
        if (frameWidth < 1 || frameHeight < 1) {
            return null
        }
        return Utils.decodeLuminanceSource(
            reader, PlanarYUVLuminanceSource(
                image,
                imageWidth,
                imageHeight,
                frameRect.left,
                frameRect.top,
                frameWidth,
                frameHeight,
                reverseHorizontal
            )
        )
    }
}
