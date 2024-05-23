package org.mjdev.libs.barcodescanner.parsers.base

import android.os.Parcelable
import org.mjdev.libs.barcodescanner.extensions.mustBe
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ParsedResultType
import java.math.BigDecimal
import java.util.*

/**
 * Base payment result class
 */
@Suppress("LeakingThis", "CanBeParameter", "MemberVisibilityCanBePrivate")
abstract class QRPaymentParsedResult(
    val rawData: String
) : ParsedResult(ParsedResultType.PRODUCT), Parcelable {

    /**
     * Amount for new transaction request
     */
    var amount: String? = null

    /**
     * Currency for new transaction request
     */
    var currency: Currency? = null

    /**
     * Invoice id for new transaction request
     */
    var invoiceId: String? = null

    /**
     * Invoice email
     */
    var email: String? = null

    /**
     * Invoice phone
     */
    var phone: String? = null

    init {
        parseData(rawData)
        checkValid()
    }

    abstract fun parseData(rawData: String)

    @Throws(Exception::class)
    fun checkValid() {
        mustBe((amount != null) && (amount!!.toBigDecimal() > BigDecimal.ZERO)) {
            "Amount can not be zero."
        }
        mustBe(currency != null) {
            "Currency parse error."
        }
    }
}
