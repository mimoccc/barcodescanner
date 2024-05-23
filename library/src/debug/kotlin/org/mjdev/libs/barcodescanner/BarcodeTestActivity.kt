package org.mjdev.libs.barcodescanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import org.mjdev.libs.barcodescanner.exception.BarcodePermissionException
import com.google.zxing.client.result.ParsedResult
import org.mjdev.libs.barcodescanner.permissions.iNeed
import org.mjdev.libs.barcodescanner.widget.BarcodeScanView

class BarcodeTestActivity : AppCompatActivity(), BarcodeScanView.ScanListener {

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val barcodeScannerView: BarcodeScanView by lazy {
        findViewById(R.id.scannerView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_barcode_reader)
        requestPermission()
    }

    override fun onResume() {
        super.onResume()
        barcodeScannerView.restart()
    }

    override fun onEvent(event: Result<ParsedResult>) {
        with(event) {
            onSuccess { result ->
                handleResultOK(result)
            }
            onFailure { error ->
                handleError(error)
            }
        }
    }

    private fun requestPermission() {
        iNeed(
            permissions = arrayOf(android.Manifest.permission.CAMERA),
            onDenied = {
                requestPermission()
            }
        )
    }

    @MainThread
    private fun restart() {
        barcodeScannerView.restart()
    }

    private fun handleResultOK(result: ParsedResult) {
        Toast.makeText(this, "QR Result OK. $result", Toast.LENGTH_LONG).show()
        postDelayed(callback = ::restart)
    }

    private fun handleError(t: Throwable) {
        when (t) {
            is BarcodePermissionException -> {
                handlePermissionMissing()
            }

            else -> {
                handleException(t)
            }
        }
    }

    private fun handleException(t: Throwable) {
        Toast.makeText(this, "QR Result ERROR: ${t.message}", Toast.LENGTH_LONG).show()
        postDelayed(callback = ::restart)
    }

    private fun handlePermissionMissing() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        Toast.makeText(
            this,
            "Please enable camera permission.",
            Toast.LENGTH_LONG
        ).show()
    }

    @Suppress("SameParameterValue")
    private fun postDelayed(millis: Long = 500, callback: () -> Unit) {
        with(handler) {
            removeCallbacks(callback)
            postDelayed(callback, millis)
        }
    }
}
