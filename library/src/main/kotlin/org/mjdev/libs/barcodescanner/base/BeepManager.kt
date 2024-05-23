package org.mjdev.libs.barcodescanner.base

import android.content.Context
import org.mjdev.libs.barcodescanner.extensions.playAssetSound
import org.mjdev.libs.barcodescanner.extensions.vibrateSingleShot

/**
 * Vibration and beep manager for scanner
 */
class BeepManager(
    private val context: Context
) {
    companion object {
        /**
         * Default vibration length
         */
        private const val VIBRATE_DURATION = 200L

        /**
         * Default sound volume
         */
        private const val SOUND_VOLUME = 0.3f

        /**
         * Default sound asset
         */
        private const val SOUND_ASSET = "beep.mp3"
    }

    private var isBeepEnabled: Boolean = false
    private var isVibrateEnabled: Boolean = false

    /**
     * Make sound if enabled and vibrate if enabled
     * Note: if device is muted sound will not be performed even it is enabled
     */
    suspend fun playBeepSoundAndVibrate() {
        if (isBeepEnabled) {
            context.playAssetSound(SOUND_ASSET, SOUND_VOLUME)
        }
        if (isVibrateEnabled) {
            context.vibrateSingleShot(VIBRATE_DURATION)
        }
    }

    /**
     * Set beep enabled/disabled
     */
    fun setBeepEnabled(enable: Boolean) {
        isBeepEnabled = enable
    }

    /**
     * Indicator if beep is enabled
     */
    fun getBeepEnabled(): Boolean {
        return isBeepEnabled
    }

    /**
     * Set vibration is enabled/disabled
     */
    fun setVibrationEnabled(enable: Boolean) {
        isVibrateEnabled = enable
    }

    /**
     * Indicator that vibration is enabled
     */
    fun getVibrationEnabled(): Boolean {
        return isVibrateEnabled
    }
}
