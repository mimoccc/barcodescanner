@file:Suppress("unused")

package org.mjdev.libs.barcodescanner.helpers

import android.os.Build
import org.mjdev.libs.barcodescanner.extensions.isNumeric
import org.mjdev.libs.barcodescanner.helpers.ISO4217CurrencyCode.Companion.currency
import java.lang.Exception
import java.util.*

object CurrencyCompat {

    /**
     * get currency code in numeric  ISO 4217 representation
     * @param currency Java Currency object
     * @return ISO 4217 numeric code
     */
    fun getNumericCode(currency: Currency?): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) currency?.numericCode
        else ISO4217CurrencyCode(currency?.currencyCode)?.code
    }

    /**
     * get currency code in numeric  ISO 4217 representation
     * @param locale locale to get currency code for
     * @return ISO 4217 numeric code
     */
    fun getNumericCode(locale: Locale): Int? {
        return try {
            val currency = Currency.getInstance(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) currency?.numericCode
            else ISO4217CurrencyCode(currency?.currencyCode)?.code
        } catch (e: Exception) {
            // some currencies are missing
            null
        }
    }

    fun fromString(currString: String?): Currency? {
        return currString?.let {
            if ((it.length == 3) && it.isNumeric()) {
                ISO4217CurrencyCode.fromCode(it)?.currency
            } else {
                Currency.getInstance(currString)
            }
        }
    }
}
