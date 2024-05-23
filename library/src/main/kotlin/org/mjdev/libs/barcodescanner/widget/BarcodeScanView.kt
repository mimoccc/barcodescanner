package org.mjdev.libs.barcodescanner.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.*
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import org.mjdev.libs.barcodescanner.exception.BarcodeNoCameraException
import org.mjdev.libs.barcodescanner.exception.BarcodePermissionException
import org.mjdev.libs.barcodescanner.extensions.hasCamera
import org.mjdev.libs.barcodescanner.extensions.hasFrontCamera
import org.mjdev.libs.barcodescanner.parsers.QRCodeMainResultParser
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import kotlinx.coroutines.launch
import org.mjdev.libs.barcodescanner.R
import org.mjdev.libs.barcodescanner.base.BeepManager
import org.mjdev.libs.barcodescanner.base.CodeScanner
import org.mjdev.libs.barcodescanner.base.Rect
import org.mjdev.libs.barcodescanner.base.ViewFinderView
import timber.log.Timber
import java.util.*
import kotlin.math.roundToInt
import kotlin.Result as KotlinResult

/**
 * Custom barcode scanner component
 * Highly customizable barcode scanner component
 * Note: This component does not need to have handled lifecycle as this component manage
 * its lifecycle itself standalone
 */
@SuppressLint("NewApi")
@Suppress("unused", "PrivatePropertyName", "MemberVisibilityCanBePrivate")
class BarcodeScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes), LifecycleEventObserver {
    private val DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY = VISIBLE
    private val DEFAULT_FLASH_BUTTON_VISIBILITY = VISIBLE
    private val DEFAULT_SWITCH_CAMERA_BUTTON_VISIBILITY = VISIBLE

    private val DEFAULT_MASK_COLOR = 0x77000000
    private val DEFAULT_FRAME_COLOR = Color.WHITE
    private val DEFAULT_AUTO_FOCUS_BUTTON_COLOR = Color.BLACK
    private val DEFAULT_FLASH_BUTTON_COLOR = Color.BLACK

    private val DEFAULT_FRAME_THICKNESS_DP = 2f
    private val DEFAULT_FRAME_ASPECT_RATIO_WIDTH = 1f
    private val DEFAULT_FRAME_ASPECT_RATIO_HEIGHT = 1f
    private val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
    private val DEFAULT_FRAME_CORNERS_RADIUS_DP = 8f
    private val DEFAULT_FRAME_SIZE = 0.75f
    private var DEFAULT_RESULT_PADDING: Int = 32

    private val BUTTON_SIZE_DP = 56f

    private val FOCUS_AREA_SIZE_DP = 20f

    private var previewView: SurfaceView
    private var viewFinderView: ViewFinderView
    private var autoFocusButton: ImageView
    private var flashButton: ImageView
    private var switchCameraButton: ImageView

    private var buttonSize = 0
    private var autoFocusButtonColor = 0
    private var flashButtonColor = 0
    private var switchCameraButtonColor = 0
    private var focusAreaSize = 0
    private var sizeListener: SizeListener? = null
    private var codeScanner: CodeScanner
    private var resultPadding: Int = 0

    private var testString: String? = "test string"
    private var scanListeners: MutableList<ScanListener> = mutableListOf()
    private var checkedCurrencies: Array<out Currency>? = null

    private val beepManager = BeepManager(context)
    private val lifecycleOwner: LifecycleOwner by lazy {
        requireNotNull(findViewTreeLifecycleOwner())
    }

    private val decodeCallBack = object : CodeScanner.DecodeCallback {
        override fun onDecoded(scanResult: Result) {
            postResult(scanResult, QRCodeMainResultParser(checkedCurrencies).parse(scanResult))
        }
    }

    private val errorCallback = object : CodeScanner.ErrorCallback {
        override fun onError(error: Throwable) {
            when (error) {
                // if is permission exception handle it
                is BarcodePermissionException -> {
                    postResult(null, kotlin.Result.failure(error))
                }
                // else another exception happen
                else -> {
                    // throw only if we already have permissions to open camera
                    // otherwise duplicate exception happen, no camera access,
                    // which is thrown when camera is unavailable and or permissions not granted
                    if (hasCameraPermission) {
                        postResult(null, kotlin.Result.failure(error))
                    }
                }
            }
        }
    }

    private var redrawOnSuccess: Boolean = false

    private val hasCamera: Boolean get() = context.hasCamera
    private val hasFrontCamera: Boolean get() = context.hasFrontCamera
    private val hasCameraPermission: Boolean
        get() = checkSelfPermission(context, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun postResult(scanResult: Result?, parsedResult: kotlin.Result<ParsedResult>) {
        post {
            runCatching {
                handleBeep()
                drawResult(if (redrawOnSuccess) scanResult else null) {
                    parsedResult.fold({
                        scanListeners.forEach { e -> e.onEvent(parsedResult) }
                    }, { error ->
                        scanListeners.forEach { e -> e.onEvent(kotlin.Result.failure(error)) }
                    })
                }
                releaseCodeScanner()
            }
        }
    }

    private fun handleBeep() {
        if (isInEditMode.not()) {
            lifecycleOwner.lifecycleScope.launch {
                beepManager.playBeepSoundAndVibrate()
            }
        }
    }

    override fun onAttachedToWindow() {
        Timber.d("Attached to window.")
        super.onAttachedToWindow()
        if (isInEditMode.not()) {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    override fun onDetachedFromWindow() {
        Timber.d("Detached from window.")
        super.onDetachedFromWindow()
        if (isInEditMode.not()) {
            lifecycleOwner.lifecycle.removeObserver(this)
        }
        releaseCodeScanner()
        removeScanListeners()
    }

    init {
        clipToPadding = false
        clipToOutline = true
        previewView = SurfaceView(context).apply {
            layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        viewFinderView = ViewFinderView(context).apply {
            layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        val density = context.resources.displayMetrics.density
        buttonSize = (density * BUTTON_SIZE_DP).roundToInt()
        focusAreaSize = (density * FOCUS_AREA_SIZE_DP).roundToInt()
        autoFocusButton = ImageView(context).apply {
            layoutParams = MarginLayoutParams(buttonSize, buttonSize)
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_autofocus_on)
            setColorFilter(DEFAULT_AUTO_FOCUS_BUTTON_COLOR)
            visibility = DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY
            setOnClickListener(AutoFocusClickListener())
        }
        flashButton = ImageView(context).apply {
            layoutParams = MarginLayoutParams(buttonSize, buttonSize)
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_flash_on)
            setColorFilter(DEFAULT_FLASH_BUTTON_COLOR)
            visibility = DEFAULT_FLASH_BUTTON_VISIBILITY
            setOnClickListener(FlashClickListener())
        }
        switchCameraButton = ImageView(context).apply {
            layoutParams = MarginLayoutParams(buttonSize, buttonSize)
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_flip_camera)
            setColorFilter(DEFAULT_FLASH_BUTTON_COLOR)
            visibility = if (hasFrontCamera) DEFAULT_SWITCH_CAMERA_BUTTON_VISIBILITY else GONE
            setOnClickListener(SwitchCameraClickListener())
        }
        codeScanner = CodeScanner(this).apply {
            setAutoFocusEnabled(true)
            setFlashEnabled(false)
            setTouchFocusEnabled(true)
            setDecodeCallback(decodeCallBack)
            setErrorCallback(errorCallback)
        }
        addView(previewView)
        addView(viewFinderView)
        addView(autoFocusButton)
        addView(flashButton)
        addView(switchCameraButton)
        setFrameAspectRatio(DEFAULT_FRAME_ASPECT_RATIO_WIDTH, DEFAULT_FRAME_ASPECT_RATIO_HEIGHT)
        setMaskColor(DEFAULT_MASK_COLOR)
        setFrameColor(DEFAULT_FRAME_COLOR)
        setFrameThickness((DEFAULT_FRAME_THICKNESS_DP * density).roundToInt())
        setFrameCornersSize((DEFAULT_FRAME_CORNER_SIZE_DP * density).roundToInt())
        setFrameCornersRadius((DEFAULT_FRAME_CORNERS_RADIUS_DP * density).roundToInt())
        setFrameSize(DEFAULT_FRAME_SIZE)
        setResultPadding(DEFAULT_RESULT_PADDING)
        initAttributes(context, attrs, defStyleAttr, defStyleRes)
        if (isInEditMode) {
            getFormats().firstOrNull()?.let { format ->
                drawResult(Result(testString, ByteArray(0), emptyArray(), format))
            }
        }
    }

    private fun initAttributes(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        context.withStyledAttributes(
            attrs,
            R.styleable.BarcodeScanView,
            defStyleAttr,
            defStyleRes
        ) {
            setMaskColor(
                getColor(
                    R.styleable.BarcodeScanView_maskColor, Color.GRAY
                )
            )
            setFrameColor(
                getColor(
                    R.styleable.BarcodeScanView_frameColor, Color.WHITE
                )
            )
            setAutoFocusButtonColor(
                getColor(
                    R.styleable.BarcodeScanView_autoFocusButtonColor, Color.BLACK
                )
            )
            setFlashButtonColor(
                getColor(
                    R.styleable.BarcodeScanView_flashButtonColor, Color.BLACK
                )
            )
            setAutoFocusButtonVisible(
                getBoolean(
                    R.styleable.BarcodeScanView_autoFocusButtonVisible, true
                )
            )
            setFlashButtonVisible(
                getBoolean(
                    R.styleable.BarcodeScanView_flashButtonVisible, true
                )
            )
            setFrameThickness(
                getDimensionPixelSize(
                    R.styleable.BarcodeScanView_frameThickness, 1
                )
            )
            setFrameCornersSize(
                getDimensionPixelSize(
                    R.styleable.BarcodeScanView_frameCornersSize, 8
                )
            )
            setFrameCornersRadius(
                getDimensionPixelSize(
                    R.styleable.BarcodeScanView_frameCornersRadius,
                    DEFAULT_FRAME_CORNERS_RADIUS_DP.toInt()
                )
            )
            setFrameAspectRatioWidth(
                getFloat(
                    R.styleable.BarcodeScanView_frameAspectRatioWidth,
                    DEFAULT_FRAME_ASPECT_RATIO_WIDTH
                )
            )
            setFrameAspectRatioHeight(
                getFloat(
                    R.styleable.BarcodeScanView_frameAspectRatioHeight,
                    DEFAULT_FRAME_ASPECT_RATIO_HEIGHT
                )
            )
            setFrameSize(
                getFloat(
                    R.styleable.BarcodeScanView_frameSize,
                    DEFAULT_FRAME_SIZE
                )
            )
            setBarcodeFormat(getInt(
                R.styleable.BarcodeScanView_barcodeFormat, 0
            ).let { id ->
                BarcodeFormat(id)
            })
            setBeepOnSuccess(
                getBoolean(
                    R.styleable.BarcodeScanView_beepOnSuccess, true
                )
            )
            setVibrateOnSuccess(
                getBoolean(
                    R.styleable.BarcodeScanView_vibrateOnSuccess, true
                )
            )
            setRedrawOnSuccess(
                getBoolean(
                    R.styleable.BarcodeScanView_redrawOnSuccess, true
                )
            )
            setCamera(getInt(
                R.styleable.BarcodeScanView_camera, 0
            ).let { id ->
                Camera(id)
            })
            setAutoFocusMode(getInt(
                R.styleable.BarcodeScanView_autoFocusMode, 0
            ).let { id ->
                AutoFocusMode(id)
            })
            setScanMode(getInt(
                R.styleable.BarcodeScanView_scanMode, 0
            ).let { id ->
                ScanMode(id)
            })
            setAutoFlashStart(
                getBoolean(
                    R.styleable.BarcodeScanView_autoFlashStart, false
                )
            )
            setZoom(
                getInt(
                    R.styleable.BarcodeScanView_zoom, 0
                )
            )
            setAutoFocusInterval(
                getInt(
                    R.styleable.BarcodeScanView_autoFocusInterval, 1000
                ).toLong()
            )
            setLaserEnabled(
                getBoolean(
                    R.styleable.BarcodeScanView_laserEnabled, true
                )
            )
            setLaserColor(
                getColor(
                    R.styleable.BarcodeScanView_laserColor, Color.RED
                )
            )
            setLaserSize(
                getDimensionPixelSize(
                    R.styleable.BarcodeScanView_laserSize, 2 // todo dp
                )
            )
            setCameraSwitchButtonVisible(
                getBoolean(R.styleable.BarcodeScanView_cameraSwitchButtonVisible, true)
            )
            setCameraSwitchButtonColor(
                getColor(R.styleable.BarcodeScanView_cameraSwitchButtonColor, Color.BLACK)
            )
            setMaskDrawable(
                getResourceId(R.styleable.BarcodeScanView_maskDrawable, -1)
            )
            setResultPadding(
                getDimensionPixelSize(
                    R.styleable.BarcodeScanView_resultPadding,
                    DEFAULT_RESULT_PADDING
                )
            )
            setTestString(
                getString(R.styleable.BarcodeScanView_testString)
            )
        }
    }

    /**
     * Set scan Listener and remove all previous
     * Listener will get errors or results when happen
     * @param listener, listener for scanner events
     */
    fun setScanListener(listener: ScanListener?) {
        if (listener != null) {
            removeScanListeners()
            addScanListener(listener)
        }
    }

    /**
     * Set scan Listener
     * Listener will get errors or results when happen
     * @param listener, listener for scanner events
     */
    fun addScanListener(listener: ScanListener?) {
        if (listener != null && (!hasScanListener(listener))) {
            scanListeners.add(listener)
        }
    }

    /**
     * Remove scan Listener
     * Listener will get errors or results when happen
     * @param listener, listener for scanner events
     */
    fun removeScanListener(listener: ScanListener?) {
        if (listener != null) {
            scanListeners.remove(listener)
        }
    }

    /**
     * Indicates that scan listeners list is not empty
     */
    fun hasScanListeners(): Boolean {
        return scanListeners.isNotEmpty()
    }

    /**
     * Indicates that scan listener is set, for concrete listener
     * @param listener, listener for scanner events
     */
    fun hasScanListener(listener: ScanListener?): Boolean {
        return listener?.let { l -> scanListeners.contains(l) } ?: false
    }

    /**
     * Remove all scan listeners
     */
    fun removeScanListeners() {
        scanListeners.clear()
    }

    /**
     * Custom string show in design mode if set as a barcode result
     * @param string, a string containing data to render as barcode
     */
    fun setTestString(string: String?) {
        testString = string
    }

    /**
     * Currencies to check if Parsed result is PaymentParsedResult
     * @param currencies currency or currencies
     */
    fun setAcceptedCurrencies(currencies: Array<Currency>?) {
        checkedCurrencies = currencies
    }

    /**
     * Return test string set from attributes or manually
     * @return string containing test data for qr code
     */
    fun getTestString(): String {
        return testString ?: ""
    }

    /**
     * Laser thickness
     * @param size thickness
     */
    fun setLaserSize(size: Int) {
        viewFinderView.setLaserSize(size)
    }

    /**
     * Laser color
     */
    fun setLaserColor(color: Int) {
        viewFinderView.setLaserColor(color)
    }

    /**
     * Laser visibility
     */
    fun setLaserEnabled(enabled: Boolean) {
        viewFinderView.setLaserEnabled(enabled)
    }

    /**
     * Button to switch front/back visibility
     * @param enabled
     */
    fun setCameraSwitchButtonVisible(enabled: Boolean) {
        switchCameraButton.visibility = if (enabled && hasFrontCamera) VISIBLE else INVISIBLE
    }

    /**
     * Button to switch cameras color
     * @param color
     */
    fun setCameraSwitchButtonColor(color: Int) {
        switchCameraButtonColor = color
        switchCameraButton.setColorFilter(color)
    }

    /**
     * Button to switch cameras color
     * @return color
     */
    fun getSwitchCameraButtonColor(): Int {
        return switchCameraButtonColor
    }

    /**
     * Get current mask color
     * @see setMaskColor
     */
    @ColorInt
    fun getMaskColor(): Int {
        return viewFinderView.getMaskColor()
    }

    /**
     * Set color of the space outside of the framing rect
     * @param color Mask color
     */
    fun setMaskColor(@ColorInt color: Int) {
        viewFinderView.setMaskColor(color)
    }

    /**
     * Get current mask color
     * @see setMaskDrawable
     */
    @DrawableRes
    fun getMaskDrawable(): Int {
        return viewFinderView.getMaskDrawable()
    }

    /**
     * Set color of the space outside of the framing rect
     * @param drawable Mask drawable
     */
    fun setMaskDrawable(@DrawableRes drawable: Int) {
        viewFinderView.setMaskDrawable(drawable)
    }

    /**
     * Get current frame color
     * @see setFrameColor
     */
    @ColorInt
    fun getFrameColor(): Int {
        return viewFinderView.getFrameColor()
    }

    /**
     * Set color of the frame
     * @param color Frame color
     */
    fun setFrameColor(@ColorInt color: Int) {
        viewFinderView.setFrameColor(color)
    }

    /**
     * Set result barcode padding inside drawable
     * @param padding padding that result will be padded to
     */
    fun setResultPadding(@Px padding: Int) {
        resultPadding = padding
    }

    /**
     * Get result barcode padding inside drawable
     * @return padding that result will be padded to
     */
    @Px
    fun getResultPadding(): Int {
        return resultPadding
    }

    /**
     * Get current frame thickness
     * @see setFrameThickness
     */
    @Px
    fun getFrameThickness(): Int {
        return viewFinderView.getFrameThickness()
    }

    /**
     * Set frame thickness
     * @param thickness Frame thickness in pixels
     */
    fun setFrameThickness(@Px thickness: Int) {
        require(thickness >= 0) { "Frame thickness can't be negative" }
        viewFinderView.setFrameThickness(thickness)
    }

    /**
     * Get current frame corners size
     * @see setFrameCornersSize
     */
    @Px
    fun getFrameCornersSize(): Int {
        return viewFinderView.getFrameCornersSize()
    }

    /**
     * Set size of the frame corners
     * @param size Size in pixels
     */
    fun setFrameCornersSize(@Px size: Int) {
        require(size >= 0) { "Frame corners size can't be negative" }
        viewFinderView.setFrameCornersSize(size)
    }

    /**
     * Get current frame corners radius
     * @see setFrameCornersRadius
     */
    @Px
    fun getFrameCornersRadius(): Int {
        return viewFinderView.getFrameCornersRadius()
    }

    /**
     * Set current frame corners radius
     * @param radius Frame corners radius in pixels
     */
    fun setFrameCornersRadius(@Px radius: Int) {
        require(radius >= 0) { "Frame corners radius can't be negative" }
        viewFinderView.setFrameCornersRadius(radius)
    }

    /**
     * Get current frame size
     * @see setFrameSize
     */
    @FloatRange(from = 0.1, to = 1.0)
    fun getFrameSize(): Float {
        return viewFinderView.getFrameSize()
    }

    /**
     * Set relative frame size where 1.0 means full size
     * @param frameSize Relative frame size between 0.1 and 1.0
     */
    fun setFrameSize(@FloatRange(from = 0.1, to = 1.0) frameSize: Float) {
//        require(frameSize in 0.1..1.0) {
//            "Max frame size value should be between 0.1 and 1, inclusive, size is: $frameSize"
//        }
        viewFinderView.setFrameSize(frameSize)
    }

    /**
     * Get current frame aspect ratio width
     * @see setFrameAspectRatioWidth
     * @see setFrameAspectRatio
     */
    @FloatRange(from = 0.0, fromInclusive = false)
    fun getFrameAspectRatioWidth(): Float {
        return viewFinderView.getFrameAspectRatioWidth()
    }

    /**
     * Set frame aspect ratio width
     * @param ratioWidth Frame aspect ratio width
     * @see setFrameAspectRatio
     */
    fun setFrameAspectRatioWidth(@FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float) {
        require(ratioWidth > 0) { "Frame aspect ratio values should be greater than zero" }
        viewFinderView.setFrameAspectRatioWidth(ratioWidth)
    }

    /**
     * Get current frame aspect ratio height
     * @see setFrameAspectRatioHeight
     * @see setFrameAspectRatio
     */
    @FloatRange(from = 0.0, fromInclusive = false)
    fun getFrameAspectRatioHeight(): Float {
        return viewFinderView.getFrameAspectRatioHeight()
    }

    /**
     * Set frame aspect ratio height
     * @param ratioHeight Frame aspect ratio width
     * @see setFrameAspectRatio
     */
    fun setFrameAspectRatioHeight(
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        require(ratioHeight > 0) { "Frame aspect ratio values should be greater than zero" }
        viewFinderView.setFrameAspectRatioHeight(ratioHeight)
    }

    /**
     * Set frame aspect ratio (ex. 1:1, 15:10, 16:9, 4:3)
     * @param ratioWidth  Frame aspect ratio width
     * @param ratioHeight Frame aspect ratio height
     */
    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        require(!(ratioWidth <= 0 || ratioHeight <= 0)) { "Frame aspect ratio values should be greater than zero" }
        viewFinderView.setFrameAspectRatio(ratioWidth, ratioHeight)
    }

    /**
     * Whether if auto focus button is currently visible
     * @see setAutoFocusButtonVisible
     */
    fun isAutoFocusButtonVisible(): Boolean {
        return autoFocusButton.visibility == VISIBLE
    }

    /**
     * Set whether auto focus button is visible or not
     * @param visible Visibility
     */
    fun setAutoFocusButtonVisible(visible: Boolean) {
        autoFocusButton.visibility = if (visible) VISIBLE else INVISIBLE
    }

    /**
     * Whether if flash button is currently visible
     * @see setFlashButtonVisible
     */
    fun isFlashButtonVisible(): Boolean {
        return flashButton.visibility == VISIBLE
    }

    /**
     * Set whether flash button is visible or not
     * @param visible Visibility
     */
    fun setFlashButtonVisible(visible: Boolean) {
        flashButton.visibility = if (visible) VISIBLE else INVISIBLE
    }

    /**
     * Get current auto focus button color
     * @see setAutoFocusButtonColor
     */
    @ColorInt
    fun getAutoFocusButtonColor(): Int {
        return autoFocusButtonColor
    }

    /**
     * Set auto focus button color
     * @param color Color
     */
    fun setAutoFocusButtonColor(@ColorInt color: Int) {
        autoFocusButtonColor = color
        autoFocusButton.setColorFilter(color)
    }

    /**
     * Get current flash button color
     * @see setFlashButtonColor
     */
    @ColorInt
    fun getFlashButtonColor(): Int {
        return flashButtonColor
    }

    /**
     * Set flash button color
     * @param color Color
     */
    fun setFlashButtonColor(@ColorInt color: Int) {
        flashButtonColor = color
        flashButton.setColorFilter(color)
    }

    /**
     * Start flash automatically when resume is called
     */
    fun setAutoFlashStart(enabled: Boolean) {
        codeScanner.setFlashEnabled(enabled)
    }

    /**
     * Set zoom
     */
    fun setZoom(zoom: Int) {
        codeScanner.setZoom(zoom)
    }

    /**
     * Set autofocus interval
     */
    fun setAutoFocusInterval(interval: Long) {
        codeScanner.setAutoFocusInterval(interval)
    }

    /**
     * Set barcode formats
     * @param format, requested format of scanned barcode
     * @note if null the all barcodes will be accepted
     */
    fun setBarcodeFormat(format: BarcodeFormat?) {
        codeScanner.setFormats(format?.value ?: BarcodeFormat.ALL.value)
    }

    /**
     * Get barcode formats
     */
    fun getFormats(): List<com.google.zxing.BarcodeFormat> {
        return codeScanner.getFormats()
    }

    /**
     * Set beep on decode
     */
    fun setBeepOnSuccess(enable: Boolean) {
        beepManager.setBeepEnabled(enable)
    }

    /**
     * Indicates that beep on successful scan is enabled
     */
    fun getBeepOnSuccess(): Boolean {
        return beepManager.getBeepEnabled()
    }

    /**
     * Set vibrate on decode
     */
    fun setVibrateOnSuccess(enable: Boolean) {
        beepManager.setVibrationEnabled(enable)
    }

    /**
     * Indicates that vibration on successful scan is enabled
     */
    fun getVibrateOnSuccess(): Boolean {
        return beepManager.getVibrationEnabled()
    }

    /**
     * Set redraw scanned code back to scan area when decoded
     */
    fun setRedrawOnSuccess(enable: Boolean) {
        redrawOnSuccess = enable
    }

    /**
     * Set camera
     * @param camera to be used
     * @note if null, back camera will be used as default
     */
    fun setCamera(camera: Camera?) {
        codeScanner.setCamera(camera?.value ?: Camera.BACK.value)
    }

    /**
     * Set autofocus mode
     * @param mode, autofocus mode to be used
     * @note if null, safe focusing will be used
     */
    fun setAutoFocusMode(mode: AutoFocusMode?) {
        codeScanner.setAutoFocusMode(mode ?: AutoFocusMode.SAFE_FOCUSING)
    }

    /**
     * Set scan mode
     * @param mode, scan mode to be used
     * @note if null, single mode, (one shot) will be used
     */
    fun setScanMode(mode: ScanMode?) {
        codeScanner.setScanMode(mode ?: ScanMode.SINGLE)
    }

    /**
     * Enable or disable autofocus
     */
    fun setAutoFocusEnabled(enabled: Boolean) {
        autoFocusButton.setImageResource(if (enabled) R.drawable.ic_autofocus_on else R.drawable.ic_autofocus_off)
    }

    /**
     * Enable or disable flash
     */
    fun setFlashEnabled(enabled: Boolean) {
        flashButton.setImageResource(if (enabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
    }

    /**
     * Return view that are previewing
     */
    fun getPreviewView(): SurfaceView {
        return previewView
    }

    /**
     * Get view that mask preview
     */
    fun getViewFinderView(): ViewFinderView {
        return viewFinderView
    }

    /**
     * Get scan frame rect
     */
    fun getFrameRect(): Rect {
        return viewFinderView.getFrameRect()
    }

    /**
     * Set size change listener
     */
    fun setSizeListener(sizeListener: SizeListener?) {
        this.sizeListener = sizeListener
    }

    /**
     * Set code scanner
     */
    fun setCodeScanner(codeScanner: CodeScanner) {
        this.codeScanner = codeScanner
        setAutoFocusEnabled(codeScanner.isAutoFocusEnabled())
        setFlashEnabled(codeScanner.isFlashEnabled())
        this.codeScanner.initialize()
    }

    /**
     * Release resources (camera)
     */
    @MainThread
    fun releaseCodeScanner() {
        try {
            codeScanner.releaseResources()
        } catch (e: Exception) {
            Timber.e(e, "Unable to release resources!!! Might cause memory leaks")
        }
    }

    /**
     * Pause
     * Note: This not need to be handled in lifecycle as this component manage its lifecycle itself  standalone
     */
    @MainThread
    fun pause() {
        if (codeScanner.isPreviewActive()) {
            codeScanner.releaseResources()
        }
    }

    /**
     * Resume
     * Note: This not need to be handled in lifecycle as this component manage its lifecycle itself  standalone
     */
    @MainThread
    fun resume() {
        if (!hasCamera) {
            lifecycleOwner.lifecycleScope.launch {
                errorCallback.onError(BarcodeNoCameraException())
            }
            return
        }
        if (!hasCameraPermission) {
            lifecycleOwner.lifecycleScope.launch {
                errorCallback.onError(BarcodePermissionException(listOf(CAMERA_PERMISSION)))
            }
            return
        }
        if (!codeScanner.isPreviewActive()) {
            codeScanner.startPreview()
        }
    }

    /**
     * Restart scanner
     */
    @MainThread
    fun restart() {
        synchronized(this) {
            pause()
            clearResult()
            resume()
        }
    }

    /**
     * This handles lifecycle
     */
    @CallSuper
    override fun onStateChanged(source: LifecycleOwner, event: Event) {
        when (event) {
            ON_PAUSE -> pause()
            ON_RESUME -> resume()
            else -> {
                // noop
            }
        }
    }

    /**
     * Handle touch events, focusing
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val codeScanner = codeScanner
        val frameRect = getFrameRect()
        val x = event.x.toInt()
        val y = event.y.toInt()
        if (
            codeScanner.isAutoFocusSupportedOrUnknown() &&
            codeScanner.isTouchFocusEnabled() &&
            (event.action == MotionEvent.ACTION_DOWN) &&
            frameRect.isPointInside(x, y)
        ) {
            val areaSize = focusAreaSize
            codeScanner.performTouchFocus(
                Rect(
                    x - areaSize,
                    y - areaSize,
                    x + areaSize,
                    y + areaSize
                ).fitIn(frameRect)
            )
        }
        return super.onTouchEvent(event)
    }

    /**
     * Draw custom result to scan area
     */
    fun drawResult(result: Result?, afterDraw: (() -> Unit)? = null) {
        viewFinderView.drawResult(result, resultPadding)
        afterDraw?.invoke()
    }

    /**
     * Clear scan area
     */
    fun clearResult() {
        viewFinderView.clearResult()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        performLayout(right - left, bottom - top)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        performLayout(width, height)
        if (isInEditMode.not()) { // thread in preview issue
            sizeListener?.onSizeChanged(width, height)
        }
    }

    private fun performLayout(width: Int, height: Int) {
        previewView.layout(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
        viewFinderView.layout(
            paddingLeft,
            paddingTop,
            width - paddingRight,
            height - paddingBottom
        )
        val buttonSize = buttonSize
        autoFocusButton.layout(
            paddingLeft,
            paddingTop,
            paddingLeft + buttonSize,
            paddingTop + buttonSize
        )
        flashButton.layout(
            width - (paddingRight + buttonSize),
            paddingTop,
            width - paddingRight,
            buttonSize + paddingTop
        )
        switchCameraButton.layout(
            (width / 2) - (buttonSize / 2),
            height - (paddingBottom + buttonSize),
            (width / 2) - (buttonSize / 2) + buttonSize,
            height - paddingBottom
        )
    }

    override fun setClipToPadding(clipToPadding: Boolean) {
        super.setClipToPadding(false)
    }

    override fun setClipToOutline(clipToOutline: Boolean) {
        super.setClipToOutline(true)
    }

    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    /**
     * Listener for view size change, to repaint ui
     */
    interface SizeListener {
        fun onSizeChanged(width: Int, height: Int)
    }

    /**
     * Button click listener
     */
    inner class AutoFocusClickListener : OnClickListener {
        override fun onClick(view: View) {
            if (!codeScanner.isAutoFocusSupportedOrUnknown()) {
                return
            }
            val enabled = !codeScanner.isAutoFocusEnabled()
            codeScanner.setAutoFocusEnabled(enabled)
            setAutoFocusEnabled(enabled)
        }
    }

    /**
     * Button click listener
     */
    inner class FlashClickListener : OnClickListener {
        override fun onClick(view: View) {
            if (codeScanner.isFlashSupportedOrUnknown()) {
                return
            }
            val enabled = !codeScanner.isFlashEnabled()
            codeScanner.setFlashEnabled(enabled)
            setFlashEnabled(enabled)
        }
    }

    /**
     * Button click listener
     */
    inner class SwitchCameraClickListener : OnClickListener {
        override fun onClick(view: View) {
            codeScanner.switchCamera()
        }
    }

    /**
     * Barcode format helper for attributes
     * An enum containing all posible scenarios to use this component
     */
    @Suppress("unused", "SpellCheckingInspection")
    enum class BarcodeFormat(val id: Int, val value: List<com.google.zxing.BarcodeFormat>) {
        /** QR Code 2D barcode format.  */
        QR_CODE(0, listOf(com.google.zxing.BarcodeFormat.QR_CODE)),

        /** Aztec 2D barcode format.  */
        AZTEC(1, listOf(com.google.zxing.BarcodeFormat.AZTEC)),

        /** CODABAR 1D format.  */
        CODABAR(2, listOf(com.google.zxing.BarcodeFormat.CODABAR)),

        /** Code 39 1D format.  */
        CODE_39(3, listOf(com.google.zxing.BarcodeFormat.CODE_39)),

        /** Code 93 1D format.  */
        CODE_93(4, listOf(com.google.zxing.BarcodeFormat.CODE_93)),

        /** Code 128 1D format.  */
        CODE_128(5, listOf(com.google.zxing.BarcodeFormat.CODE_128)),

        /** Data Matrix 2D barcode format.  */
        DATA_MATRIX(6, listOf(com.google.zxing.BarcodeFormat.DATA_MATRIX)),

        /** EAN-8 1D format.  */
        EAN_8(7, listOf(com.google.zxing.BarcodeFormat.EAN_8)),

        /** EAN-13 1D format.  */
        EAN_13(8, listOf(com.google.zxing.BarcodeFormat.EAN_13)),

        /** ITF (Interleaved Two of Five) 1D format.  */
        ITF(9, listOf(com.google.zxing.BarcodeFormat.ITF)),

        /** MaxiCode 2D barcode format.  */
        MAXICODE(10, listOf(com.google.zxing.BarcodeFormat.MAXICODE)),

        /** PDF417 format.  */
        PDF_417(11, listOf(com.google.zxing.BarcodeFormat.PDF_417)),

        /** RSS 14  */
        RSS_14(12, listOf(com.google.zxing.BarcodeFormat.RSS_14)),

        /** RSS EXPANDED  */
        RSS_EXPANDED(13, listOf(com.google.zxing.BarcodeFormat.RSS_EXPANDED)),

        /** UPC-A 1D format.  */
        UPC_A(14, listOf(com.google.zxing.BarcodeFormat.UPC_A)),

        /** UPC-E 1D format.  */
        UPC_E(15, listOf(com.google.zxing.BarcodeFormat.UPC_E)),

        /** UPC/EAN extension format. Not a stand-alone format.  */
        UPC_EAN_EXTENSION(16, listOf(com.google.zxing.BarcodeFormat.UPC_EAN_EXTENSION)),

        /** One dimensional formats **/
        ONE_DIMENSIONAL(
            17, listOf(
                com.google.zxing.BarcodeFormat.CODABAR,
                com.google.zxing.BarcodeFormat.CODE_39,
                com.google.zxing.BarcodeFormat.CODE_93,
                com.google.zxing.BarcodeFormat.CODE_128,
                com.google.zxing.BarcodeFormat.EAN_8,
                com.google.zxing.BarcodeFormat.EAN_13,
                com.google.zxing.BarcodeFormat.ITF,
                com.google.zxing.BarcodeFormat.RSS_14,
                com.google.zxing.BarcodeFormat.RSS_EXPANDED,
                com.google.zxing.BarcodeFormat.UPC_A,
                com.google.zxing.BarcodeFormat.UPC_E,
                com.google.zxing.BarcodeFormat.UPC_EAN_EXTENSION
            )
        ),

        /** Two dimensional formats **/
        TWO_DIMENSIONAL(
            18, listOf(
                com.google.zxing.BarcodeFormat.AZTEC,
                com.google.zxing.BarcodeFormat.DATA_MATRIX,
                com.google.zxing.BarcodeFormat.MAXICODE,
                com.google.zxing.BarcodeFormat.PDF_417,
                com.google.zxing.BarcodeFormat.QR_CODE
            )
        ),

        /** All supported formats **/
        ALL(
            19, listOf(
                com.google.zxing.BarcodeFormat.AZTEC,
                com.google.zxing.BarcodeFormat.CODABAR,
                com.google.zxing.BarcodeFormat.CODE_128,
                com.google.zxing.BarcodeFormat.CODE_39,
                com.google.zxing.BarcodeFormat.CODE_93,
                com.google.zxing.BarcodeFormat.DATA_MATRIX,
                com.google.zxing.BarcodeFormat.EAN_13,
                com.google.zxing.BarcodeFormat.EAN_8,
                com.google.zxing.BarcodeFormat.ITF,
                com.google.zxing.BarcodeFormat.MAXICODE,
                com.google.zxing.BarcodeFormat.PDF_417,
                com.google.zxing.BarcodeFormat.RSS_14,
                com.google.zxing.BarcodeFormat.UPC_A,
                com.google.zxing.BarcodeFormat.UPC_E,
                com.google.zxing.BarcodeFormat.UPC_EAN_EXTENSION,
                com.google.zxing.BarcodeFormat.QR_CODE,
                com.google.zxing.BarcodeFormat.RSS_EXPANDED
            )
        );

        companion object {
            operator fun invoke(value: Int) = entries.find { it.id == value }
        }
    }

    /**
     * Scan mode
     */
    @Suppress("unused")
    enum class ScanMode(val id: Int) {
        /**
         * Preview will stop after first decoded code
         */
        SINGLE(0),

        /**
         * Continuous scan, don't stop preview after decoding the code
         */
        CONTINUOUS(1);

        companion object {
            operator fun invoke(value: Int) = entries.find { it.id == value }
        }
    }

    /**
     * Autofocus mode
     */
    @Suppress("unused")
    enum class AutoFocusMode(val id: Int) {
        /**
         * Auto focus camera with the specified interval
         *
         * @see CodeScanner#setAutoFocusInterval(long)
         */
        SAFE_FOCUSING(0),

        /**
         * Continuous auto focus, may not work on some devices
         */
        CONTINUOUS_FOCUSING(1);

        companion object {
            operator fun invoke(value: Int) = entries.find { it.id == value }
        }
    }

    /**
     * Default camera
     */
    @Suppress("unused")
    enum class Camera(val id: Int, val value: Int) {
        FRONT(0, CodeScanner.CAMERA_FRONT),
        BACK(1, CodeScanner.CAMERA_BACK);

        companion object {
            operator fun invoke(value: Int) = entries.find { it.id == value }
        }
    }

    /**
     * Scan result listener
     * This is used as event for model view controller and to handle state of this component
     */
    interface ScanListener {
        fun onEvent(event: KotlinResult<ParsedResult>)
    }
}
