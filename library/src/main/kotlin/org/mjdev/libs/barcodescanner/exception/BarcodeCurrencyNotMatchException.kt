package org.mjdev.libs.barcodescanner.exception

import org.mjdev.libs.barcodescanner.parsers.base.QRPaymentParsedResult
import java.util.*

/**
 * Barcode format exception
 * Raised when payment currency not match
 */
class BarcodeCurrencyNotMatchException(
    var result: QRPaymentParsedResult
) : RuntimeException() {
    val currency: Currency? get() = result.currency
}
