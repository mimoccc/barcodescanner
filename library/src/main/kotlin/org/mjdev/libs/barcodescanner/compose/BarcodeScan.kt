package org.mjdev.libs.barcodescanner.compose

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.client.result.ParsedResult
import org.mjdev.libs.barcodescanner.R
import org.mjdev.libs.barcodescanner.widget.BarcodeScanView

@Suppress("unused")
@Composable
fun BarcodeScan(
    modifier: Modifier = Modifier,
    autoFlashAtStart: Boolean = false,
    autoFocusButtonVisible: Boolean = true,
    redrawCodeOnSuccess: Boolean = true,
    beepOnSuccess: Boolean = false,
    vibrateOnSuccess: Boolean = true,
    cameraSwitchButtonVisible: Boolean = true,
    flashButtonVisible: Boolean = true,
    laserEnabled: Boolean = true,
    autoFocusButtonColor: Color = Color.White,
    cameraSwitchButtonColor: Color = Color.White,
    flashButtonColor: Color = Color.White,
    frameColor: Color = Color.White,
    laserColor: Color = Color.White,
    maskColor: Color = Color.DarkGray,
    maskDrawable: Int = R.drawable.barcode_reader_mask,
    @FloatRange(from = 0.1, to = 1.0)
    frameSize: Float = 0.65f,
    @FloatRange(from = 0.1)
    frameAspectRatioHeight: Float = 1.0f,
    @FloatRange(from = 0.1)
    frameAspectRatioWidth: Float = 1.0f,
    frameCornersRadius: Dp = 4.dp,
    frameCornersSize: Dp = 20.dp,
    frameThickness: Dp = 4.dp,
    laserSize: Dp = 2.dp,
    resultPadding: Dp = 24.dp,
    autoFocusInterval: Long = 1000,
    zoom: Int = 1,
    autoFocusMode: BarcodeScanView.AutoFocusMode = BarcodeScanView.AutoFocusMode.SAFE_FOCUSING,
    startOnCamera: BarcodeScanView.Camera = BarcodeScanView.Camera.BACK,
    barcodeFormat: BarcodeScanView.BarcodeFormat = BarcodeScanView.BarcodeFormat.QR_CODE,
    scanMode: BarcodeScanView.ScanMode = BarcodeScanView.ScanMode.SINGLE,
    currencies: Array<java.util.Currency>? = null,
    localDensity: Density = LocalDensity.current,
    testString: String = "",
    scanListener: (Result<ParsedResult>) -> Unit = {}
) = AndroidView(
    modifier = modifier,
    factory = { context ->
        BarcodeScanView(context)
    }
) { view ->
    with(localDensity) {
        with(view) {
            setMaskColor(maskColor.toArgb())
            setLaserColor(laserColor.toArgb())
            setFrameColor(frameColor.toArgb())
            setFlashButtonColor(flashButtonColor.toArgb())
            setCameraSwitchButtonColor(cameraSwitchButtonColor.toArgb())
            setAutoFocusButtonColor(autoFocusButtonColor.toArgb())
            setLaserSize(laserSize.roundToPx())
            setFrameThickness(frameThickness.roundToPx())
            setFrameCornersSize(frameCornersSize.roundToPx())
            setFrameCornersRadius(frameCornersRadius.roundToPx())
            setResultPadding(resultPadding.roundToPx())
            setFrameSize(frameSize)
            setMaskDrawable(maskDrawable)
            setLaserEnabled(laserEnabled)
            setFrameAspectRatioWidth(frameAspectRatioWidth)
            setFrameAspectRatioHeight(frameAspectRatioHeight)
            setFlashButtonVisible(flashButtonVisible)
            setCameraSwitchButtonVisible(cameraSwitchButtonVisible)
            setBeepOnSuccess(beepOnSuccess)
            setAutoFocusButtonVisible(autoFocusButtonVisible)
            setAutoFocusInterval(autoFocusInterval)
            setAutoFocusMode(autoFocusMode)
            setCamera(startOnCamera)
            setAutoFlashStart(autoFlashAtStart)
            setRedrawOnSuccess(redrawCodeOnSuccess)
            setBarcodeFormat(barcodeFormat)
            setAcceptedCurrencies(currencies)
            setVibrateOnSuccess(vibrateOnSuccess)
            setTestString(testString)
            setScanMode(scanMode)
            setZoom(zoom)
            setScanListener(object : BarcodeScanView.ScanListener {
                override fun onEvent(event: Result<ParsedResult>) {
                    scanListener(event)
                }
            })
        }
    }
}

@Preview(device = Devices.NEXUS_5)
@Composable
fun QrCodeScannerPreview() {
    BarcodeScan(
        modifier = Modifier,
        testString = "mailto:mimoccc@gmail.com?subject=Hello&body=World!"
    )
}