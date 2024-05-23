package org.mjdev.libs.barcodescanner.parsers.types

import org.mjdev.libs.barcodescanner.extensions.mustBe
import org.mjdev.libs.barcodescanner.parsers.base.QRCBAParsedResult
import org.mjdev.libs.barcodescanner.parsers.base.QRPaymentParser
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * QR code parser for CBA SPD (CZ)
 */
class QRCBASPDParser : QRPaymentParser() {

    @Throws(Exception::class)
    override fun parse(result: Result?): ParsedResult? {
        return try {
            val rawText = getMassagedText(result)
            mustBe(rawText.startsWith("SPD*")) {
                "SPD header missing"
            }
            QRCBASPDParsedResult(rawText)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    /**
     * QR code parser result for CBA SPD
     */
    @Suppress("CanBeParameter")
    @Parcelize
    class QRCBASPDParsedResult(private val text: String) : QRCBAParsedResult(text)
}
