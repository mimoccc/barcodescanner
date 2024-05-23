package org.mjdev.libs.barcodescanner.bysquare.data

import org.mjdev.libs.barcodescanner.bysquare.base.BysquareDocument
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems.InvoiceLines

open class InvoiceItemsBase : BysquareDocument {
    override var invoiceId: String? = null
    var firstInvoiceLineID: String? = null
    var invoiceLines: InvoiceLines? = null

    override val amount: Double
        get() = invoiceLines?.amount ?: 0.0

    override val currency: String? = null

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
