package org.mjdev.libs.barcodescanner.exception

/**
 * Barcode format exception
 * Raised when barcode can not be recognized
 */
@Suppress("unused")
class BarcodeDataException(
    reason: String = "Unknown reason.",
    cause: Throwable? = null
) : RuntimeException(reason, cause)
