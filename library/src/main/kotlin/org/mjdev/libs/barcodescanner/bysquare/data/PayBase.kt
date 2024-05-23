package org.mjdev.libs.barcodescanner.bysquare.data

import org.mjdev.libs.barcodescanner.bysquare.base.BysquareDocument
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.data.pay.Payments

open class PayBase : BysquareDocument {
    override var invoiceId: String? = null
    var payments: Payments? = null

    override val amount: Double
        get() = try {
            payments?.amount ?: 0.0
        } catch (e: Exception) {
            0.0
        }

    override val currency: String?
        get() = payments?.currency

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
