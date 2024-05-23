// for compatibility with android 5.0+ we use old camera api
// not all 5.0+ devices have cam api v2, or not fully supported
@file:Suppress("deprecation")

package org.mjdev.libs.barcodescanner.base

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Handler
import android.os.Process
import android.view.SurfaceHolder
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import org.mjdev.libs.barcodescanner.base.Utils.configureDefaultFocusArea
import org.mjdev.libs.barcodescanner.base.Utils.configureFocusArea
import org.mjdev.libs.barcodescanner.base.Utils.configureFocusModeForTouch
import org.mjdev.libs.barcodescanner.base.Utils.configureFpsRange
import org.mjdev.libs.barcodescanner.base.Utils.configureSceneMode
import org.mjdev.libs.barcodescanner.base.Utils.configureVideoStabilization
import org.mjdev.libs.barcodescanner.base.Utils.disableAutoFocus
import org.mjdev.libs.barcodescanner.base.Utils.findSuitableImageSize
import org.mjdev.libs.barcodescanner.base.Utils.getDisplayOrientation
import org.mjdev.libs.barcodescanner.base.Utils.getImageFrameRect
import org.mjdev.libs.barcodescanner.base.Utils.getPreviewSize
import org.mjdev.libs.barcodescanner.base.Utils.isPortrait
import org.mjdev.libs.barcodescanner.base.Utils.setAutoFocusMode
import org.mjdev.libs.barcodescanner.base.Utils.setFlashMode
import org.mjdev.libs.barcodescanner.base.Utils.setZoom
import org.mjdev.libs.barcodescanner.widget.BarcodeScanView
import org.mjdev.libs.barcodescanner.exception.CodeScannerException
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import java.util.Objects.requireNonNull

/**
 * CodeScanner, associated with the first back-facing camera on the device
 * @param scannerView A scannerView to display the preview
 * @see BarcodeScanView
 */
@Suppress("unused", "PrivatePropertyName")
class CodeScanner(
    val scannerView: BarcodeScanView
) {
    companion object {
        /**
         * First back-facing camera
         */
        const val CAMERA_BACK = 0

        /**
         * First front-facing camera
         */
        const val CAMERA_FRONT = 1
    }

    private val DEFAULT_FORMATS = BarcodeScanView.BarcodeFormat.ALL.value
    private val DEFAULT_SCAN_MODE = BarcodeScanView.ScanMode.SINGLE
    private val DEFAULT_AUTO_FOCUS_MODE = BarcodeScanView.AutoFocusMode.SAFE_FOCUSING

    private val DEFAULT_AUTO_FOCUS_ENABLED = true
    private val DEFAULT_TOUCH_FOCUS_ENABLED = true
    private val DEFAULT_FLASH_ENABLED = false
    private val DEFAULT_SAFE_AUTO_FOCUS_INTERVAL = 2000L
    private val SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD = 2

    private val context: Context = scannerView.context
    private val initializeLock = Any()
    private var mainThreadHandler: Handler = Handler()
    private var surfaceHolder: SurfaceHolder = scannerView.getPreviewView().holder
    private var surfaceCallback: SurfaceHolder.Callback
    private var previewCallback: Camera.PreviewCallback
    private var touchFocusCallback: Camera.AutoFocusCallback
    private var safeAutoFocusCallback: Camera.AutoFocusCallback
    private var safeAutoFocusTask: Runnable
    private var stopPreviewTask: Runnable
    private var decoderStateListener: DecoderStateListener
    private var formats = DEFAULT_FORMATS
    private var scanMode = DEFAULT_SCAN_MODE
    private var autoFocusMode = DEFAULT_AUTO_FOCUS_MODE
    private var decodeCallback: DecodeCallback? = null
    private var errorCallback: ErrorCallback? = null
    private var decoderWrapper: DecoderWrapper? = null
    private var initialization = false
    private var initialized = false
    private var stoppingPreview = false
    private var autoFocusEnabled = DEFAULT_AUTO_FOCUS_ENABLED
    private var flashEnabled = DEFAULT_FLASH_ENABLED
    private var safeAutoFocusInterval = DEFAULT_SAFE_AUTO_FOCUS_INTERVAL
    private var cameraId = CAMERA_BACK
    private var zoom = 0
    private var touchFocusEnabled = DEFAULT_TOUCH_FOCUS_ENABLED
    private var touchFocusing = false
    private var previewActive = false
    private var safeAutoFocusing = false
    private var safeAutoFocusTaskScheduled = false
    private var initializationRequested = false
    private var safeAutoFocusAttemptsCount = 0
    private var viewWidth = 0
    private var viewHeight = 0

    init {
        mainThreadHandler = Handler()
        surfaceCallback = SurfaceCallback()
        previewCallback = PreviewCallback()
        touchFocusCallback = TouchFocusCallback()
        safeAutoFocusCallback = SafeAutoFocusCallback()
        safeAutoFocusTask = SafeAutoFocusTask()
        stopPreviewTask = StopPreviewTask()
        decoderStateListener = DecoderStateListener()
        scannerView.setCodeScanner(this)
        scannerView.setSizeListener(ScannerSizeListener())
    }

    /**
     * Get current camera id, or [CAMERA_BACK] or [CAMERA_FRONT]
     * @see setCamera
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getCamera(): Int {
        return cameraId
    }

    /**
     * Camera to use
     * @param cameraId Camera id (between `0` and
     * [Camera.getNumberOfCameras] - `1`)
     * or [CAMERA_BACK] or [CAMERA_FRONT]
     */
    fun setCamera(cameraId: Int) {
        synchronized(initializeLock) {
            if (this.cameraId != cameraId) {
                this.cameraId = cameraId
                if (initialized) {
                    val previewActive = previewActive
                    releaseResources()
                    if (previewActive) {
                        initialize()
                    }
                }
            }
        }
    }

    /**
     * Switch back and front camera
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun switchCamera() {
        setCamera(
            when (getCamera()) {
                CAMERA_BACK -> CAMERA_FRONT
                CAMERA_FRONT -> CAMERA_BACK
                else -> CAMERA_BACK
            }
        )
    }

    /**
     * Get current list of formats to decode
     * @see setFormats
     */
    fun getFormats(): List<BarcodeFormat> {
        return formats
    }

    /**
     * Formats, decoder to react to
     * @param formats Formats
     * @see BarcodeFormat
     */
    fun setFormats(formats: List<BarcodeFormat>) {
        synchronized(initializeLock) {
            this.formats = requireNonNull(formats)
            if (initialized) {
                val decoderWrapper = decoderWrapper
                decoderWrapper?.getDecoder()?.setFormats(formats)
            }
        }
    }

    /**
     * Get current decode callback
     * @see setDecodeCallback
     */
    fun getDecodeCallback(): DecodeCallback? {
        return decodeCallback
    }

    /**
     * Callback of decoding process
     * @param decodeCallback Callback
     * @see DecodeCallback
     */
    fun setDecodeCallback(decodeCallback: DecodeCallback?) {
        synchronized(initializeLock) {
            this.decodeCallback = decodeCallback
            if (initialized) {
                val decoderWrapper = decoderWrapper
                decoderWrapper?.getDecoder()?.setCallback(decodeCallback)
            }
        }
    }

    /**
     * Get current error callback
     * @see setErrorCallback
     */
    fun getErrorCallback(): ErrorCallback? {
        return errorCallback
    }

    /**
     * Camera initialization error callback.
     * If not set, an exception will be thrown when error will occur.
     * @param errorCallback Callback
     * @see ErrorCallback.SUPPRESS
     * @see ErrorCallback
     */
    fun setErrorCallback(errorCallback: ErrorCallback?) {
        this.errorCallback = errorCallback
    }

    /**
     * Get current scan mode
     * @see setScanMode
     * @return scan mode currently selected
     */
    fun getScanMode(): BarcodeScanView.ScanMode {
        return scanMode
    }

    /**
     * Scan mode, [BarcodeScanView.ScanMode.SINGLE] by default
     * @see BarcodeScanView.ScanMode
     * @param scanMode scan mode
     */
    fun setScanMode(scanMode: BarcodeScanView.ScanMode) {
        this.scanMode = requireNonNull(scanMode)
    }

    /**
     * Get current zoom value
     * @return zoom level
     */
    fun getZoom(): Int {
        return zoom
    }

    /**
     * Set current zoom value (between `0` and [Camera.Parameters.getMaxZoom], if larger,
     * max zoom value will be set
     * @param zoom zoom level
     */
    fun setZoom(zoom: Int) {
        require(zoom >= 0) { "Zoom value must be greater than or equal to zero" }
        synchronized(initializeLock) {
            if (zoom != this.zoom) {
                this.zoom = zoom
                if (initialized) {
                    val decoderWrapper = decoderWrapper
                    if (decoderWrapper != null) {
                        val camera = decoderWrapper.getCamera()
                        val parameters = camera.parameters
                        setZoom(parameters, zoom)
                        camera.parameters = parameters
                    }
                }
            }
        }
        this.zoom = zoom
    }

    /**
     * Touch focus is currently enabled or not
     * @return
     */
    fun isTouchFocusEnabled(): Boolean {
        return touchFocusEnabled
    }

    /**
     * Enable or disable touch focus. If enabled, touches inside viewfinder frame will cause focusing into
     * specified area, auto focus will be switched off.
     */
    fun setTouchFocusEnabled(touchFocusEnabled: Boolean) {
        this.touchFocusEnabled = touchFocusEnabled
    }

    /**
     * Auto focus is currently enabled or not
     * @see setAutoFocusEnabled
     */
    fun isAutoFocusEnabled(): Boolean {
        return autoFocusEnabled
    }

    /**
     * Enable or disable auto focus if it's supported, `true` by default
     * @param autoFocusEnabled value
     */
    @MainThread
    fun setAutoFocusEnabled(autoFocusEnabled: Boolean) {
        synchronized(initializeLock) {
            val changed = this.autoFocusEnabled != autoFocusEnabled
            this.autoFocusEnabled = autoFocusEnabled
            scannerView.setAutoFocusEnabled(autoFocusEnabled)
            val decoderWrapper = decoderWrapper
            if (initialized && previewActive && changed && decoderWrapper != null && decoderWrapper.isAutoFocusSupported()) {
                setAutoFocusEnabledInternal(autoFocusEnabled)
            }
        }
    }

    /**
     * Get current auto focus mode
     * @see setAutoFocusMode
     */
    fun getAutoFocusMode(): BarcodeScanView.AutoFocusMode {
        return autoFocusMode
    }

    /**
     * Auto focus mode, [BarcodeScanView.AutoFocusMode.SAFE_FOCUSING] by default
     * @see BarcodeScanView.AutoFocusMode
     */
    @MainThread
    fun setAutoFocusMode(autoFocusMode: BarcodeScanView.AutoFocusMode) {
        synchronized(initializeLock) {
            this.autoFocusMode = requireNonNull(autoFocusMode)
            if (initialized && autoFocusEnabled) {
                setAutoFocusEnabledInternal(true)
            }
        }
    }

    /**
     * Auto focus interval in milliseconds for [BarcodeScanView.AutoFocusMode.SAFE_FOCUSING] mode, 2000 by default
     * @see setAutoFocusMode
     */
    @MainThread
    fun setAutoFocusInterval(autoFocusInterval: Long) {
        safeAutoFocusInterval = autoFocusInterval
    }

    /**
     * Flash light is currently enabled or not
     */
    fun isFlashEnabled(): Boolean {
        return flashEnabled
    }

    /**
     * Enable or disable flash light if it's supported, `false` by default
     */
    @MainThread
    fun setFlashEnabled(flashEnabled: Boolean) {
        synchronized(initializeLock) {
            val changed = this.flashEnabled != flashEnabled
            this.flashEnabled = flashEnabled
            scannerView.setFlashEnabled(flashEnabled)
            val decoderWrapper = decoderWrapper
            if (initialized && previewActive && changed && decoderWrapper != null && decoderWrapper.isFlashSupported()) {
                setFlashEnabledInternal(flashEnabled)
            }
        }
    }

    /**
     * Preview is active or not
     */
    fun isPreviewActive(): Boolean {
        return previewActive
    }

    /**
     * Start camera preview
     * <br></br>
     * Requires [Manifest.permission.CAMERA] permission
     */
    @MainThread
    fun startPreview() {
        synchronized(initializeLock) {
            if (!initialized && !initialization) {
                initialize()
                return
            }
        }
        if (!previewActive) {
            surfaceHolder.addCallback(surfaceCallback)
            startPreviewInternal(false)
        }
    }

    /**
     * Stop camera preview
     */
    @MainThread
    fun stopPreview() {
        if (initialized && previewActive) {
            surfaceHolder.removeCallback(surfaceCallback)
            stopPreviewInternal(false)
        }
    }

    /**
     * Release resources, and stop preview if needed
     */
    @MainThread
    fun releaseResources() {
        if (initialized) {
            if (previewActive) {
                stopPreview()
            }
            releaseResourcesInternal()
        }
    }

    /**
     * Perform touch to focus camera while scanning codes
     * @param viewFocusArea area where focussing is about to prepare
     */
    fun performTouchFocus(viewFocusArea: Rect?) {
        synchronized(initializeLock) {
            if (initialized && previewActive && !touchFocusing) {
                try {
                    setAutoFocusEnabled(false)
                    val decoderWrapper = decoderWrapper
                    if (previewActive && decoderWrapper != null && decoderWrapper.isAutoFocusSupported()) {
                        val imageSize = decoderWrapper.getImageSize()
                        var imageWidth = imageSize.x
                        var imageHeight = imageSize.y
                        val orientation = decoderWrapper.getDisplayOrientation()
                        if (orientation == 90 || orientation == 270) {
                            val width = imageWidth
                            imageWidth = imageHeight
                            imageHeight = width
                        }
                        val imageArea = getImageFrameRect(
                            imageWidth,
                            imageHeight,
                            viewFocusArea!!,
                            decoderWrapper.getPreviewSize(),
                            decoderWrapper.getViewSize()
                        )
                        val camera = decoderWrapper.getCamera()
                        camera.cancelAutoFocus()
                        val parameters = camera.parameters
                        configureFocusArea(
                            parameters,
                            imageArea,
                            imageWidth,
                            imageHeight,
                            orientation
                        )
                        configureFocusModeForTouch(parameters)
                        camera.parameters = parameters
                        camera.autoFocus(touchFocusCallback)
                        touchFocusing = true
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    /**
     * Indicator about autofocus support for current camera
     * @return true if autofocus is supported for selected camera
     */
    fun isAutoFocusSupportedOrUnknown(): Boolean {
        val wrapper = decoderWrapper
        return wrapper == null || wrapper.isAutoFocusSupported()
    }

    /**
     * Indicator about flash support for current camera
     * @return true if flash is available
     */
    fun isFlashSupportedOrUnknown(): Boolean {
        val wrapper = decoderWrapper
        return wrapper == null || wrapper.isFlashSupported()
    }

    fun initialize() {
        initialize(scannerView.width, scannerView.height)
    }

    fun initialize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        if (width > 0 && height > 0) {
            initialization = true
            initializationRequested = false
            InitializationThread(width, height).start()
        } else {
            initializationRequested = true
        }
    }

    private fun startPreviewInternal(internal: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.getCamera()
                camera.setPreviewCallback(previewCallback)
                camera.setPreviewDisplay(surfaceHolder)
                if (!internal && decoderWrapper.isFlashSupported() && flashEnabled) {
                    setFlashEnabledInternal(true)
                }
                camera.startPreview()
                stoppingPreview = false
                previewActive = true
                safeAutoFocusing = false
                safeAutoFocusAttemptsCount = 0
                if (decoderWrapper.isAutoFocusSupported() && autoFocusEnabled) {
                    val frameRect = scannerView.getFrameRect()
                    val parameters = camera.parameters
                    configureDefaultFocusArea(parameters, decoderWrapper, frameRect)
                    camera.parameters = parameters
                    if (autoFocusMode == BarcodeScanView.AutoFocusMode.SAFE_FOCUSING) {
                        scheduleSafeAutoFocusTask()
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun startPreviewInternalSafe() {
        if (initialized && !previewActive) {
            startPreviewInternal(true)
        }
    }

    private fun stopPreviewInternal(internal: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.getCamera()
                camera.cancelAutoFocus()
                val parameters = camera.parameters
                if (!internal && decoderWrapper.isFlashSupported() && flashEnabled) {
                    setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF)
                }
                camera.parameters = parameters
                camera.setPreviewCallback(null)
                camera.stopPreview()
            }
        } catch (ignored: Exception) {
        }
        stoppingPreview = false
        previewActive = false
        safeAutoFocusing = false
        safeAutoFocusAttemptsCount = 0
    }

    private fun stopPreviewInternalSafe() {
        if (initialized && previewActive) {
            stopPreviewInternal(true)
        }
    }

    private fun releaseResourcesInternal() {
        initialized = false
        initialization = false
        stoppingPreview = false
        previewActive = false
        safeAutoFocusing = false
        val decoderWrapper = decoderWrapper
        if (decoderWrapper != null) {
            this.decoderWrapper = null
            decoderWrapper.release()
        }
    }

    private fun setFlashEnabledInternal(flashEnabled: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.getCamera()
                val parameters = camera.parameters ?: return
                if (flashEnabled) {
                    setFlashMode(parameters, Camera.Parameters.FLASH_MODE_TORCH)
                } else {
                    setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF)
                }
                camera.parameters = parameters
            }
        } catch (ignored: Exception) {
        }
    }

    private fun setAutoFocusEnabledInternal(autoFocusEnabled: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.getCamera()
                camera.cancelAutoFocus()
                touchFocusing = false
                val parameters = camera.parameters
                val autoFocusMode = autoFocusMode
                if (autoFocusEnabled) {
                    setAutoFocusMode(parameters, autoFocusMode)
                } else {
                    disableAutoFocus(parameters)
                }
                if (autoFocusEnabled) {
                    val frameRect = scannerView.getFrameRect()
                    configureDefaultFocusArea(parameters, decoderWrapper, frameRect)
                }
                camera.parameters = parameters
                if (autoFocusEnabled) {
                    safeAutoFocusAttemptsCount = 0
                    safeAutoFocusing = false
                    if (autoFocusMode == BarcodeScanView.AutoFocusMode.SAFE_FOCUSING) {
                        scheduleSafeAutoFocusTask()
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    /**
     * Safely autofocus camera
     */
    @MainThread
    fun safeAutoFocusCamera() {
        if (!initialized || !previewActive) {
            return
        }
        val decoderWrapper = decoderWrapper
        if (decoderWrapper == null || !decoderWrapper.isAutoFocusSupported() ||
            !autoFocusEnabled
        ) {
            return
        }
        if (safeAutoFocusing && safeAutoFocusAttemptsCount < SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD) {
            safeAutoFocusAttemptsCount++
        } else {
            try {
                val camera = decoderWrapper.getCamera()
                camera.cancelAutoFocus()
                camera.autoFocus(safeAutoFocusCallback)
                safeAutoFocusAttemptsCount = 0
                safeAutoFocusing = true
            } catch (e: Exception) {
                safeAutoFocusing = false
            }
        }
        scheduleSafeAutoFocusTask()
    }

    private fun scheduleSafeAutoFocusTask() {
        if (safeAutoFocusTaskScheduled) {
            return
        }
        safeAutoFocusTaskScheduled = true
        mainThreadHandler.postDelayed(safeAutoFocusTask, safeAutoFocusInterval)
    }

    /**
     * Size change handler
     */
    inner class ScannerSizeListener : BarcodeScanView.SizeListener {
        override fun onSizeChanged(width: Int, height: Int) {
            synchronized(initializeLock) {
                if (width != viewWidth || height != viewHeight) {
                    val previewActive: Boolean = previewActive
                    if (initialized) {
                        releaseResources()
                    }
                    if (previewActive || initializationRequested) {
                        initialize(width, height)
                    }
                }
            }
        }
    }

    /**
     * Preview callback
     * It decodes every picture taken, to found barcode
     */
    @Suppress("OVERRIDE_DEPRECATION")
    inner class PreviewCallback : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray?, camera: Camera) {
            if (!initialized || stoppingPreview || data == null) {
                return
            }
            val decoderWrapper: DecoderWrapper = decoderWrapper
                ?: return
            val decoder = decoderWrapper.getDecoder()
            if (decoder.getState() != Decoder.State.IDLE) {
                return
            }
            val frameRect: Rect = scannerView.getFrameRect()
            if (frameRect.width < 1 || frameRect.height < 1) {
                return
            }
            decoder.decode(
                DecodeTask(
                    data, decoderWrapper.getImageSize(),
                    decoderWrapper.getPreviewSize(), decoderWrapper.getViewSize(), frameRect,
                    decoderWrapper.getDisplayOrientation(),
                    decoderWrapper.shouldReverseHorizontal()
                )
            )
        }
    }

    /**
     * Surface callback for surface settings
     */
    inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            startPreviewInternalSafe()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (holder.surface == null) {
                previewActive = false
                return
            }
            stopPreviewInternalSafe()
            startPreviewInternalSafe()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            stopPreviewInternalSafe()
        }
    }

    /**
     * Listener to decoder states
     */
    inner class DecoderStateListener : Decoder.StateListener {
        override fun onStateChanged(state: Decoder.State): Boolean {
            if (state == Decoder.State.DECODED) {
                val scanMode: BarcodeScanView.ScanMode = scanMode
                if (scanMode == BarcodeScanView.ScanMode.SINGLE) {
                    stoppingPreview = true
                    mainThreadHandler.post(stopPreviewTask)
                }
            }
            return true
        }
    }

    /**
     * Initialization thread called everytime settings change
     */
    inner class InitializationThread(
        private val mWidth: Int,
        private val mHeight: Int
    ) : Thread("cs-init") {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            try {
                initialize()
            } catch (e: Exception) {
                releaseResourcesInternal()
                val errorCallback: ErrorCallback? = errorCallback
                if (errorCallback != null) {
                    errorCallback.onError(e)
                } else {
                    throw e
                }
            }
        }

        private fun initialize() {
            var camera: Camera? = null
            val cameraInfo = Camera.CameraInfo()
            val cameraId: Int = cameraId
            if (cameraId == CAMERA_BACK || cameraId == CAMERA_FRONT) {
                val numberOfCameras = Camera.getNumberOfCameras()
                val facing = if (cameraId == CAMERA_BACK)
                    Camera.CameraInfo.CAMERA_FACING_BACK
                else
                    Camera.CameraInfo.CAMERA_FACING_FRONT
                for (i in 0 until numberOfCameras) {
                    Camera.getCameraInfo(i, cameraInfo)
                    if (cameraInfo.facing == facing) {
                        camera = Camera.open(i)
                        this@CodeScanner.cameraId = i
                        break
                    }
                }
            } else {
                camera = Camera.open(cameraId)
                Camera.getCameraInfo(cameraId, cameraInfo)
            }
            if (camera == null) {
                throw CodeScannerException("Unable to access camera")
            }
            val parameters =
                camera.parameters ?: throw CodeScannerException("Unable to configure camera")
            val orientation = getDisplayOrientation(context, cameraInfo)
            val portrait = isPortrait(orientation)
            val imageSize = findSuitableImageSize(
                parameters,
                if (portrait) mHeight else mWidth,
                if (portrait) mWidth else mHeight
            )
            val imageWidth = imageSize.x
            val imageHeight = imageSize.y
            parameters.setPreviewSize(imageWidth, imageHeight)
            parameters.previewFormat = ImageFormat.NV21
            val previewSize = getPreviewSize(
                if (portrait) imageHeight else imageWidth,
                if (portrait) imageWidth else imageHeight, mWidth, mHeight
            )
            val focusModes = parameters.supportedFocusModes
            val autoFocusSupported = focusModes != null &&
                    (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ||
                            focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            if (!autoFocusSupported) {
                autoFocusEnabled = false
            }
            val viewSize = Point(mWidth, mHeight)
            if (autoFocusSupported && autoFocusEnabled) {
                setAutoFocusMode(parameters, autoFocusMode)
                val frameRect: Rect = scannerView.getFrameRect()
                configureDefaultFocusArea(
                    parameters,
                    frameRect,
                    previewSize,
                    viewSize,
                    imageWidth,
                    imageHeight,
                    orientation
                )
            }
            val flashModes = parameters.supportedFlashModes
            val flashSupported =
                flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)
            if (!flashSupported) {
                flashEnabled = false
            }
            val zoom: Int = zoom
            if (zoom != 0) {
                setZoom(parameters, zoom)
            }
            configureFpsRange(parameters)
            configureSceneMode(parameters)
            configureVideoStabilization(parameters)
            camera.parameters = parameters
            camera.setDisplayOrientation(orientation)
            synchronized(initializeLock) {
                val decoder = Decoder(
                    decoderStateListener,
                    formats,
                    decodeCallback
                )
                decoderWrapper = DecoderWrapper(
                    camera,
                    cameraInfo,
                    decoder,
                    imageSize,
                    previewSize,
                    viewSize,
                    orientation,
                    autoFocusSupported,
                    flashSupported
                )
                decoder.start()
                initialization = false
                initialized = true
            }
            mainThreadHandler.post(FinishInitializationTask())
        }
    }

    /**
     * Touch focus callback to handle touches
     */
    inner class TouchFocusCallback : Camera.AutoFocusCallback {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onAutoFocus(success: Boolean, camera: Camera) {
            touchFocusing = false
        }
    }

    /**
     * Autofocus callback to handle focusing
     */
    inner class SafeAutoFocusCallback : Camera.AutoFocusCallback {
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onAutoFocus(success: Boolean, camera: Camera) {
            safeAutoFocusing = false
        }
    }

    /**
     * Autofocus task, called when focusing
     */
    inner class SafeAutoFocusTask : Runnable {
        override fun run() {
            safeAutoFocusTaskScheduled = false
            if (autoFocusMode == BarcodeScanView.AutoFocusMode.SAFE_FOCUSING) {
                safeAutoFocusCamera()
            }
        }
    }

    /**
     * Stop preview task called when stopping camera
     */
    inner class StopPreviewTask : Runnable {
        override fun run() {
            stopPreview()
        }
    }

    /**
     * Initialization finish task
     * This task starts preview
     */
    inner class FinishInitializationTask : Runnable {
        override fun run() {
            if (!initialized) {
                return
            }
            scannerView.setAutoFocusEnabled(isAutoFocusEnabled())
            scannerView.setFlashEnabled(isFlashEnabled())
            startPreview()
        }
    }

    /**
     * Decoding callback when barcode is found
     */
    interface DecodeCallback {
        /**
         * Called when decoder has successfully decoded the code
         * <br></br>
         * Note that this method always called on a worker thread
         * @param scanResult Encapsulates the result of decoding a barcode within an image
         */
        @WorkerThread
        fun onDecoded(scanResult: Result)
    }

    /**
     * Error callback
     */
    interface ErrorCallback {
        /**
         * Called when error has occurred
         * <br></br>
         * Note that this method always called on a worker thread
         * @param error Exception that has been thrown
         */
        @WorkerThread
        fun onError(error: Throwable)

        companion object {
            /**
             * Callback to suppress errors
             */
            @JvmField
            val SUPPRESS: ErrorCallback = Utils.SuppressErrorCallback()
        }
    }
}