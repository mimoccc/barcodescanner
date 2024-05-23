package org.mjdev.libs.barcodescanner.exception

/**
 * Barcode format exception
 * Raised when barcode can not be recognized
 */
@Suppress("unused")
class BarcodePermissionException(var permissions: List<String>) : RuntimeException()
