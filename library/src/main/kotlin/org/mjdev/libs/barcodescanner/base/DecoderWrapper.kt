@file:Suppress("deprecation")
package org.mjdev.libs.barcodescanner.base

import android.hardware.Camera

/**
 * Decoder camera wrapper
 * Takes care of camera for decoding
 */
@Suppress("unused")
class DecoderWrapper(
    camera: Camera,
    cameraInfo: Camera.CameraInfo,
    decoder: Decoder,
    imageSize: Point,
    previewSize: Point,
    viewSize: Point,
    displayOrientation: Int,
    autoFocusSupported: Boolean,
    flashSupported: Boolean
) {
    private var mCamera: Camera = camera
    private var mCameraInfo: Camera.CameraInfo = cameraInfo
    private var mDecoder: Decoder = decoder
    private var mImageSize: Point = imageSize
    private var mPreviewSize: Point = previewSize
    private var mViewSize: Point = viewSize
    private var mDisplayOrientation: Int = displayOrientation
    private var mReverseHorizontal: Boolean = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
    private var mAutoFocusSupported: Boolean = autoFocusSupported
    private var mFlashSupported: Boolean = flashSupported

    /**
     * Return current camera
     * @return camera used
     */
    fun getCamera(): Camera {
        return mCamera
    }

    /**
     * Return camera information's
     * @see Camera.CameraInfo
     * @return camera information's
     */
    fun getCameraInfo(): Camera.CameraInfo {
        return mCameraInfo
    }

    /**
     * Return current decoder
     * @See Decoder
     * @return decoder
     */
    fun getDecoder(): Decoder {
        return mDecoder
    }

    /**
     * Return image size processed
     * @See Point
     * @return point
     */
    fun getImageSize(): Point {
        return mImageSize
    }

    /**
     * Returns preview size
     * @See Point
     * @return point
     */
    fun getPreviewSize(): Point {
        return mPreviewSize
    }

    /**
     * Get view size for viewfinder
     * @See Point
     * @return point
     */
    fun getViewSize(): Point {
        return mViewSize
    }

    /**
     * Returns device display orientation
     * @return int that contains device orientation information
     */
    fun getDisplayOrientation(): Int {
        return mDisplayOrientation
    }

    /**
     * Indicator if reversing horizontal is available
     */
    fun shouldReverseHorizontal(): Boolean {
        return mReverseHorizontal
    }

    /**
     * Indicator about autofocus support
     */
    fun isAutoFocusSupported(): Boolean {
        return mAutoFocusSupported
    }

    /**
     * Indicator about flash support
     */
    fun isFlashSupported(): Boolean {
        return mFlashSupported
    }

    /**
     * Release resources (camera)
     */
    fun release() {
        mCamera.release()
        mDecoder.shutdown()
    }
}
