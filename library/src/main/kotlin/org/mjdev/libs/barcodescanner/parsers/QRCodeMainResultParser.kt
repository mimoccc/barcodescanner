package org.mjdev.libs.barcodescanner.parsers

import org.mjdev.libs.barcodescanner.exception.BarcodeCurrencyNotMatchException
import org.mjdev.libs.barcodescanner.exception.BarcodeDataException
import org.mjdev.libs.barcodescanner.extensions.isInArray
import org.mjdev.libs.barcodescanner.extensions.mustBe
import org.mjdev.libs.barcodescanner.parsers.base.QRPaymentParsedResult
import org.mjdev.libs.barcodescanner.parsers.types.QRBySquareParser
import org.mjdev.libs.barcodescanner.parsers.types.QRCBASIDParser
import org.mjdev.libs.barcodescanner.parsers.types.QRCBASPDParser
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import java.util.*

/**
 * Main QR Payment qr code parser
 * This parser handle all qr code payments
 */
class QRCodeMainResultParser(
    private val checkedCurrencies: Array<out Currency>? = null
) {

    companion object {
        /**
         * List of parsers that parse payments
         */
        @Suppress("Unused")
        val PAYMENTS_PARSERS = QRCodeMainResultParser().PAYMENTS_PARSERS
    }

    /**
     * List of parsers that parse payments, with filtered currencies
     */
    @Suppress("PropertyName", "unused")
    val PAYMENTS_PARSERS = listOf(
        // CBA SID parser
        QRCBASIDParser(),
        // CBA SPD parser
        QRCBASPDParser(),
        // BySquare QR code parser
        QRBySquareParser()
    )

    /**
     * Parse result from scan data
     * This is main function to traverse through formats to select one right,
     * that represents data scanned
     */
    fun parse(theResult: Result): kotlin.Result<ParsedResult> = runCatching {
        var payment: ParsedResult? = null
        // cycle all payment parsers, to find right one
        for (payParser in PAYMENTS_PARSERS) {
            payment = payParser.parse(theResult)
            // if is success
            if (payment is QRPaymentParsedResult) {
                // check currency is in list of supported currencies
                mustBe(payment.currency.isInArray(checkedCurrencies)) {
                    // throw error if not
                    BarcodeCurrencyNotMatchException(payment)
                }
                // We have result
                break
            }
        }
        // payment must be not null, otherwise it is no payment qr code or unsupported
        payment ?: throw (BarcodeDataException("No Payment QR code found"))
    }
}
