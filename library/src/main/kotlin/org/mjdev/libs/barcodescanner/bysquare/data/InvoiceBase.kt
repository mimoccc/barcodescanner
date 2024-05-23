package org.mjdev.libs.barcodescanner.bysquare.data

import org.mjdev.libs.barcodescanner.bysquare.base.BysquareDocument
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.CustomerParty
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.MonetarySummary
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.PaymentMean
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.SingleInvoiceLine
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.SupplierParty
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.TaxCategorySummaries
import java.util.*

open class InvoiceBase : BysquareDocument {
    override var invoiceId: String? = null
    var issueDate: Date? = null
    var taxPointDate: Date? = null
    var orderID: String? = null
    var deliveryNoteID: String? = null
    var localCurrencyCode: String? = null
    var foreignCurrencyCode: String? = null
    var currRate: Double? = null
    var referenceCurrRate: Double? = null
    var supplierParty: SupplierParty? = null
    var customerParty: CustomerParty? = null
    var numberOfInvoiceLines: Int? = null
    var invoiceDescription: String? = null
    var singleInvoiceLine: SingleInvoiceLine? = null
    var taxCategorySummaries: TaxCategorySummaries? = null
    var monetarySummary: MonetarySummary? = null
    var paymentMeans: List<PaymentMean>? = null

    override val currency: String?
        get() = foreignCurrencyCode ?: localCurrencyCode

    override val amount: Double
        get() {
            val quantity = singleInvoiceLine?.invoicedQuantity ?: 0.0
            val tax: Double = taxCategorySummaries?.tax ?: 0.0
            return quantity + tax
        }

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
