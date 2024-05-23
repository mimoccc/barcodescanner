package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify
import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceBase
import java.util.*

@Suppress("unused")
class SingleInvoiceLine : IVerifiable {
    var deliveryNoteLineID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var invoice: InvoiceBase? = null
    var invoicedQuantity: Double = 0.0
    var itemEANCode: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var itemName: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var orderLineID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var periodFromDate: Date? = null
    var periodToDate: Date? = null

    val unitPriceTaxAmount: Double
        get() = if (invoice != null && invoicedQuantity != 0.0)
            invoice!!.monetarySummary!!.taxAmount / invoicedQuantity
        else 0.0

    val unitPriceTaxExclusiveAmount: Double
        get() = if (invoice != null && invoicedQuantity != 0.0)
            invoice!!.monetarySummary!!.taxExclusiveAmount / invoicedQuantity
        else 0.0

    val unitPriceTaxInclusiveAmount: Double
        get() = if (invoice != null && invoicedQuantity != 0.0)
            invoice!!.monetarySummary!!.taxInclusiveAmount / invoicedQuantity
        else 0.0

    val isEmpty: Boolean
        get() = orderLineID == null && deliveryNoteLineID == null && itemName == null &&
                itemEANCode == null && periodFromDate == null && periodToDate == null &&
                invoicedQuantity == 0.0

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.choice("ItemName ItemEANCode", arrayOf<Any?>(itemName, itemEANCode))
        Verify.periodFromToDate(periodFromDate, periodToDate)
    }
}
