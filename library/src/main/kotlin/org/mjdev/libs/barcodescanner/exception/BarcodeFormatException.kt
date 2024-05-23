package org.mjdev.libs.barcodescanner.exception

import com.google.zxing.client.result.ParsedResult

/**
 * Barcode format exception
 * Raised when barcode can not be recognized
 */
@Suppress("unused")
class BarcodeFormatException(
    val result: ParsedResult
) : RuntimeException()
