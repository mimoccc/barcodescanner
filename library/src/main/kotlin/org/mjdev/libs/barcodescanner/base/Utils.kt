@file:Suppress("deprecation")
package org.mjdev.libs.barcodescanner.base

import android.content.Context
import android.hardware.Camera
import android.view.Surface
import android.view.WindowManager
import org.mjdev.libs.barcodescanner.widget.BarcodeScanView
import org.mjdev.libs.barcodescanner.exception.CodeScannerException
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Utils {
    private const val MIN_DISTORTION = 0.3f
    private const val MAX_DISTORTION = 3f
    private const val DISTORTION_STEP = 0.1f
    private const val MIN_PREVIEW_PIXELS = 589824
    private const val MIN_FPS = 10000
    private const val MAX_FPS = 30000

    /**
     * Check and manage aspect of an preview
     */
    fun findSuitableImageSize(
        parameters: Camera.Parameters,
        frameWidth: Int,
        frameHeight: Int
    ): Point {
        val sizes = parameters.supportedPreviewSizes
        if (sizes != null && sizes.isNotEmpty()) {
            Collections.sort(sizes, CameraSizeComparator())
            val frameRatio = frameWidth.toFloat() / frameHeight.toFloat()
            var distortion = MIN_DISTORTION
            while (distortion <= MAX_DISTORTION) {
                for (size in sizes) {
                    val width = size.width
                    val height = size.height
                    if (width * height >= MIN_PREVIEW_PIXELS &&
                        abs(frameRatio - width.toFloat() / height.toFloat()) <= distortion
                    ) {
                        return Point(width, height)
                    }
                }
                distortion += DISTORTION_STEP
            }
        }
        val defaultSize = parameters.previewSize
            ?: throw CodeScannerException("Unable to configure camera preview size")
        return Point(defaultSize.width, defaultSize.height)
    }

    /**
     * Configure framing
     */
    fun configureFpsRange(parameters: Camera.Parameters) {
        val supportedFpsRanges = parameters.supportedPreviewFpsRange
        if (supportedFpsRanges == null || supportedFpsRanges.isEmpty()) {
            return
        }
        Collections.sort(supportedFpsRanges, FpsRangeComparator())
        for (fpsRange in supportedFpsRanges) {
            if (fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] >= MIN_FPS &&
                fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] <= MAX_FPS
            ) {
                parameters.setPreviewFpsRange(
                    fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                    fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                )
                return
            }
        }
    }

    /**
     * Configure scene
     */
    fun configureSceneMode(parameters: Camera.Parameters) {
        if (Camera.Parameters.SCENE_MODE_BARCODE != parameters.sceneMode) {
            val supportedSceneModes = parameters.supportedSceneModes
            if (supportedSceneModes != null &&
                supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_BARCODE)
            ) {
                parameters.sceneMode = Camera.Parameters.SCENE_MODE_BARCODE
            }
        }
    }

    /**
     * Configure video stabilisation for focusing
     */
    fun configureVideoStabilization(parameters: Camera.Parameters) {
        if (parameters.isVideoStabilizationSupported && !parameters.videoStabilization) {
            parameters.videoStabilization = true
        }
    }

    /**
     * Configure focusing
     */
    fun configureFocusArea(
        parameters: Camera.Parameters,
        area: Rect,
        width: Int,
        height: Int,
        orientation: Int
    ) {
        val areas: MutableList<Camera.Area> = ArrayList(1)
        val rotatedArea =
            area.rotate(-orientation.toFloat(), width / 2f, height / 2f).bound(0, 0, width, height)
        areas.add(
            Camera.Area(
                android.graphics.Rect(
                    mapCoordinate(rotatedArea.left, width),
                    mapCoordinate(rotatedArea.top, height),
                    mapCoordinate(rotatedArea.right, width),
                    mapCoordinate(rotatedArea.bottom, height)
                ), 1000
            )
        )
        if (parameters.maxNumFocusAreas > 0) {
            parameters.focusAreas = areas
        }
        if (parameters.maxNumMeteringAreas > 0) {
            parameters.meteringAreas = areas
        }
    }

    /**
     * Configure focusing
     */
    fun configureDefaultFocusArea(
        parameters: Camera.Parameters,
        frameRect: Rect, previewSize: Point,
        viewSize: Point, width: Int, height: Int,
        orientation: Int
    ) {
        val portrait = isPortrait(orientation)
        val rotatedWidth = if (portrait) height else width
        val rotatedHeight = if (portrait) width else height
        configureFocusArea(
            parameters,
            getImageFrameRect(rotatedWidth, rotatedHeight, frameRect, previewSize, viewSize),
            rotatedWidth, rotatedHeight, orientation
        )
    }

    /**
     * Configure focusing
     */
    fun configureDefaultFocusArea(
        parameters: Camera.Parameters,
        decoderWrapper: DecoderWrapper,
        frameRect: Rect
    ) {
        val imageSize = decoderWrapper.getImageSize()
        configureDefaultFocusArea(
            parameters, frameRect, decoderWrapper.getPreviewSize(),
            decoderWrapper.getViewSize(), imageSize.x, imageSize.y,
            decoderWrapper.getDisplayOrientation()
        )
    }

    /**
     * Configure focusing by touch
     */
    fun configureFocusModeForTouch(parameters: Camera.Parameters) {
        if (Camera.Parameters.FOCUS_MODE_AUTO == parameters.focusMode) {
            return
        }
        val focusModes = parameters.supportedFocusModes
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
    }

    /**
     * Disable autofocus
     */
    fun disableAutoFocus(parameters: Camera.Parameters) {
        val focusModes = parameters.supportedFocusModes
        if (focusModes == null || focusModes.isEmpty()) {
            return
        }
        val focusMode = parameters.focusMode
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            if (Camera.Parameters.FOCUS_MODE_FIXED == focusMode) {
                return
            } else {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
                return
            }
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            if (Camera.Parameters.FOCUS_MODE_AUTO != focusMode) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
        }
    }

    /**
     * Configure autofocus
     */
    fun setAutoFocusMode(
        parameters: Camera.Parameters,
        autoFocusMode: BarcodeScanView.AutoFocusMode
    ) {
        val focusModes = parameters.supportedFocusModes
        if (focusModes == null || focusModes.isEmpty()) {
            return
        }
        if (autoFocusMode === BarcodeScanView.AutoFocusMode.CONTINUOUS_FOCUSING) {
            if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE == parameters.focusMode) {
                return
            }
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                return
            }
        }
        if (Camera.Parameters.FOCUS_MODE_AUTO == parameters.focusMode) {
            return
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
    }

    /**
     * Configure flash
     */
    fun setFlashMode(
        parameters: Camera.Parameters,
        flashMode: String
    ) {
        if (flashMode == parameters.flashMode) {
            return
        }
        val flashModes = parameters.supportedFlashModes
        if (flashModes != null && flashModes.contains(flashMode)) {
            parameters.flashMode = flashMode
        }
    }

    /**
     * Configure zoom
     */
    fun setZoom(parameters: Camera.Parameters, zoom: Int) {
        if (parameters.isZoomSupported) {
            if (parameters.zoom != zoom) {
                val maxZoom = parameters.maxZoom
                if (zoom <= maxZoom) {
                    parameters.zoom = zoom
                } else {
                    parameters.zoom = maxZoom
                }
            }
        }
    }

    /**
     * Get display details, orientation
     */
    fun getDisplayOrientation(
        context: Context,
        cameraInfo: Camera.CameraInfo
    ): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            ?: throw CodeScannerException("Unable to access window manager")
        val degrees: Int
        val rotation = windowManager.defaultDisplay.rotation
        degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> if (rotation % 90 == 0) {
                (360 + rotation) % 360
            } else {
                throw CodeScannerException("Invalid display rotation")
            }
        }
        return ((if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) 180 else 360) +
                cameraInfo.orientation - degrees) % 360
    }

    /**
     * Indicator that device is handled in portrait mode
     */
    fun isPortrait(orientation: Int): Boolean {
        return orientation == 90 || orientation == 270
    }

    /**
     * Preview size
     */
    fun getPreviewSize(
        imageWidth: Int,
        imageHeight: Int,
        frameWidth: Int,
        frameHeight: Int
    ): Point {
        if (imageWidth == frameWidth && imageHeight == frameHeight) {
            return Point(frameWidth, frameHeight)
        }
        val resultWidth = imageWidth * frameHeight / imageHeight
        return if (resultWidth < frameWidth) {
            Point(frameWidth, imageHeight * frameWidth / imageWidth)
        } else {
            Point(resultWidth, frameHeight)
        }
    }

    /**
     * Image frame size
     */
    fun getImageFrameRect(
        imageWidth: Int,
        imageHeight: Int,
        viewFrameRect: Rect,
        previewSize: Point,
        viewSize: Point
    ): Rect {
        val previewWidth = previewSize.x
        val previewHeight = previewSize.y
        val viewWidth = viewSize.x
        val viewHeight = viewSize.y
        val wD = (previewWidth - viewWidth) / 2
        val hD = (previewHeight - viewHeight) / 2
        val wR = imageWidth.toFloat() / previewWidth.toFloat()
        val hR = imageHeight.toFloat() / previewHeight.toFloat()
        return Rect(
            max(((viewFrameRect.left + wD) * wR).roundToInt(), 0),
            max(((viewFrameRect.top + hD) * hR).roundToInt(), 0),
            min(((viewFrameRect.right + wD) * wR).roundToInt(), imageWidth),
            min(((viewFrameRect.bottom + hD) * hR).roundToInt(), imageHeight)
        )
    }

    /**
     * Color balancing
     */
    fun rotateYuv(
        source: ByteArray, width: Int, height: Int,
        rotation: Int
    ): ByteArray {
        if (rotation == 0 || rotation == 360) {
            return source
        }
        require(!(rotation % 90 != 0 || rotation < 0 || rotation > 270)) { "Invalid rotation (valid: 0, 90, 180, 270)" }
        val output = ByteArray(source.size)
        val frameSize = width * height
        val swap = rotation % 180 != 0
        val flipX = rotation % 270 != 0
        val flipY = rotation >= 180
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIn = j * width + i
                val uIn = frameSize + (j shr 1) * width + (i and 1.inv())
                val vIn = uIn + 1
                val wOut = if (swap) height else width
                val hOut = if (swap) width else height
                val iSwapped = if (swap) j else i
                val jSwapped = if (swap) i else j
                val iOut = if (flipX) wOut - iSwapped - 1 else iSwapped
                val jOut = if (flipY) hOut - jSwapped - 1 else jSwapped
                val yOut = jOut * wOut + iOut
                val uOut = frameSize + (jOut shr 1) * wOut + (iOut and 1.inv())
                val vOut = uOut + 1
                output[yOut] = (0xff and source[yIn].toInt()).toByte()
                output[uOut] = (0xff and source[uIn].toInt()).toByte()
                output[vOut] = (0xff and source[vIn].toInt()).toByte()
            }
        }
        return output
    }

    /**
     * Color balancing
     */
    @Throws(ReaderException::class)
    fun decodeLuminanceSource(
        reader: MultiFormatReader,
        luminanceSource: LuminanceSource
    ): Result? {
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(luminanceSource)))
        } catch (e: NotFoundException) {
            reader.decodeWithState(
                BinaryBitmap(HybridBinarizer(luminanceSource.invert()))
            )
        } finally {
            reader.reset()
        }
    }

    private fun mapCoordinate(value: Int, size: Int): Int {
        return 2000 * value / size - 1000
    }

    /**
     * Custom callback suppressing error
     */
    internal class SuppressErrorCallback : CodeScanner.ErrorCallback {
        override fun onError(error: Throwable) {
            // Do nothing
        }
    }

    /**
     * Comparator for camera sizes
     */
    internal class CameraSizeComparator : Comparator<Camera.Size> {
        override fun compare(a: Camera.Size, b: Camera.Size): Int {
            return (b.height * b.width).compareTo(a.height * a.width)
        }
    }

    /**
     * Comparator of frequencies
     */
    internal class FpsRangeComparator : Comparator<IntArray> {
        override fun compare(a: IntArray, b: IntArray): Int {
            var comparison = b[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
                .compareTo(a[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
            if (comparison == 0) {
                comparison = b[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
                    .compareTo(a[Camera.Parameters.PREVIEW_FPS_MIN_INDEX])
            }
            return comparison
        }
    }
}
