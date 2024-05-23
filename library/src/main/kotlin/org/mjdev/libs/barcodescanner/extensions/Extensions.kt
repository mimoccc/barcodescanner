@file:Suppress("DEPRECATION", "unused")

package org.mjdev.libs.barcodescanner.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import org.mjdev.libs.barcodescanner.exception.BarcodeDataException
import java.lang.UnsupportedOperationException
import java.util.*

/**
 * Normal ringer mode
 */
@Suppress("unused")
const val RINGER_MODE_NORMAL = android.media.AudioManager.RINGER_MODE_NORMAL

/**
 * Silent ringer mode
 */
@Suppress("unused")
const val RINGER_MODE_SILENT = android.media.AudioManager.RINGER_MODE_SILENT

/**
 * Audio manager used to play tone when scanning
 */
fun Context.audioManager(): AudioManager {
    return getSystemService(Context.AUDIO_SERVICE) as AudioManager
}

/**
 * Vibration manager used to vibrate code scanning
 */
@RequiresApi(Build.VERSION_CODES.S)
fun Context.vibratorManager(): VibratorManager {
    return getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
}

@Suppress("deprecation")
fun Context.defaultVibrator(): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibratorManager().defaultVibrator
    } else {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }
}

/**
 * Function indicates if device have audio enabled for concrete mode
 * @param inMode audio mode, default RINGER_MODE_NORMAL
 * @return boolean value, true if enabled
 */
fun Context.hasRingerMode(inMode: Int): Boolean {
    val ringerMode = audioManager().ringerMode
    val enabled = (ringerMode == inMode).apply {
        Timber.i(
            "sound ${
                if (this) "enabled"
                else "disabled"
            } for current ringer mode: $ringerMode"
        )
    }
    return enabled
}

/**
 * Play an asset sound file
 * @param soundFileName sound file name in assets
 * @param volume optional volume parameter
 * @param inMode optional audio mode parameter, default RINGER_MODE_NORMAL
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun Context.playAssetSound(
    soundFileName: String,
    volume: Float = 1f,
    inMode: Int = AudioManager.RINGER_MODE_NORMAL
) {
    withContext(Dispatchers.IO) {
        try {
            val ringerMode = audioManager().ringerMode
            val shouldPlayBeep = ringerMode == inMode
            if (!shouldPlayBeep) {
                Timber.i("sound disabled for current ringer mode: $ringerMode")
                return@withContext
            }
            MediaPlayer().apply {
                isLooping = false
                setVolume(volume, volume)
                setOnCompletionListener { release() }
                setOnPreparedListener { start() }
                with(assets.openFd(soundFileName)) {
                    setDataSource(fileDescriptor, startOffset, length)
                    close()
                }
                prepare()
            }
        } catch (e: java.lang.Exception) {
            Timber.e(e, "Unable to play sound $soundFileName")
        }
    }
}

/**
 * One and only vibrate method. By default vibrate is enabled only when not in silent mode.
 *
 * @param duration duration of vibration
 * @param effect Effect of the vibration -> only for targeting API Android Q >= 29
 * */
@SuppressLint("WrongConstant")
@Suppress("deprecation")
suspend fun Context.vibrateSingleShot(
    duration: Long = 200L,
    effect: Int? = null
) {
    if (hasRingerMode(AudioManager.RINGER_MODE_SILENT)) {
        Timber.d("Vibrate disabled because of silent mode")
        return
    }
    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                defaultVibrator().vibrate(
                    if (effect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(effect)
                    } else {
                        VibrationEffect.createOneShot(
                            duration,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    }
                )
            } else {
                defaultVibrator().vibrate(duration)
            }
        } catch (e: Throwable) {
            Timber.e(e, "Unable to vibrate")
        }
    }
}

/**
 * Check if list has duplicates
 * @receiver list any type
 * @return boolean value indicates duplicity in list
 */
fun <T> List<T>.hasDuplicates(): Boolean {
    return size != distinct().count()
}

/**
 * Check if string is numeric value representation
 * @receiver string to test
 * @return returns true if string contains only numbers
 */
fun String.isNumeric(): Boolean {
    return matches("-?\\d+(\\.\\d+)?".toRegex())
}

/**
 * Convert string to currency representation
 * @return currency or null
 * @note: it supports also numeric 4217 iso code as input
 */
@SuppressLint("ObsoleteSdkInt")
fun String.toCurrencyOrNull(): Currency? = try {
    if (isNumeric()) {
        Integer.parseInt(this).let { isoCode ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    Currency.getAvailableCurrencies().firstOrNull {
                        it.numericCode == isoCode
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    throw(UnsupportedOperationException("Non supported iso code."))
                }
                else -> {
                    throw(UnsupportedOperationException("Non supported iso code."))
                }
            }
        }
    } else {
        Currency.getInstance(this)
    }
} catch (e: Exception) {
    null
}

/**
 * System indicator that camera physicaly exists
 */
val Context.systemHasCamera: Boolean
    get() = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

/**
 * Cameras count
 */
val Context.cameraCount: Int get() = if (systemHasCamera) Camera.getNumberOfCameras() else 0

/**
 * Indicator that device have even one camer
 */
val Context.hasCamera: Boolean get() = cameraCount > 0

/**
 * Indicator that device have front camera
 */
@Suppress("DEPRECATED_IDENTITY_EQUALS")
val Context.hasFrontCamera: Boolean
    get() {
        var hasFrontCamera = false
        if (hasCamera) {
            try {
                val cameraInfo = Camera.CameraInfo()
                val numberOfCameras: Int = Camera.getNumberOfCameras()
                for (i in 0 until numberOfCameras) {
                    Camera.getCameraInfo(i, cameraInfo)
                    if (cameraInfo.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        hasFrontCamera = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return hasFrontCamera
    }

/**
 * Check condition and prepare BarcodeException from string or raise another exception
 * The exception is raised when condition is false
 * @param lazyError exception or string
 * Note: If string is returned from lazy fnc, it will be converted to BarcodeDataException
 */
inline fun mustBe(value: Boolean, lazyError: () -> Any) {
    if (!value) {
        val message = lazyError()
        throw if (message is Exception) message else BarcodeDataException(message.toString())
    }
}

/**
 * Check if item is existing in array
 * @param array to check item exists in
 * @param nullMeansOK if set to true and array is null return always true
 * @param emptyMeansOK if set to true and array is empty return always true
 * @return true if condition match
 */
inline fun <reified T> T.isInArray(
    array: Array<out T>?,
    nullMeansOK: Boolean = true,
    emptyMeansOK: Boolean = false
): Boolean {
    return when {
        (array == null) -> return nullMeansOK
        array.isEmpty() -> return emptyMeansOK
        else -> array.contains(this)
    }
}
