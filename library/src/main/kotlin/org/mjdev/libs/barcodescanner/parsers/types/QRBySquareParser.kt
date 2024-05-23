package org.mjdev.libs.barcodescanner.parsers.types

import org.mjdev.libs.barcodescanner.bysquare.base.BysquareDocument
import org.mjdev.libs.barcodescanner.bysquare.base.Header
import org.mjdev.libs.barcodescanner.bysquare.base.Utils
import org.mjdev.libs.barcodescanner.helpers.CurrencyCompat
import org.mjdev.libs.barcodescanner.parsers.base.QRPaymentParsedResult
import org.mjdev.libs.barcodescanner.parsers.base.QRPaymentParser
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * QR Payment qr code parser for BySquare (SK)
 */
class QRBySquareParser : QRPaymentParser() {

    @Throws(Exception::class)
    override fun parse(result: Result?): ParsedResult? {
        return try {
            val rawText = getMassagedText(result)
            QRBySquareParsedResult(rawText)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    @Suppress("CanBeParameter")
    @Parcelize
    class QRBySquareParsedResult(private val text: String) : QRPaymentParsedResult(text) {
        override fun parseData(rawData: String) {
            val bySquareData = parseRawData(rawData)
            invoiceId = bySquareData.invoiceId ?: ""
            amount = bySquareData.amount.toString()
            currency = bySquareData.currency?.let { CurrencyCompat.fromString(it) }
        }

        private fun parseRawData(text: String): BysquareDocument {
            val dataDecoded = Utils.decodeBase32HexString(text)
            val header = Header.decodeHeader(Utils.copyOf(dataDecoded, 2))
            return header.parse(Utils.copyOfRange(dataDecoded, 2, dataDecoded.size - 2))
        }

        override fun getDisplayResult(): String {
            val result = StringBuilder(100)
            maybeAppend(amount, result)
            maybeAppend(currency?.currencyCode, result)
            maybeAppend(invoiceId, result)
            return result.toString()
        }
    }
}
