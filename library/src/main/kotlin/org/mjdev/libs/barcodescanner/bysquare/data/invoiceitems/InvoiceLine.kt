package org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class InvoiceLine : IVerifiable {
    var classifiedTaxCategory: Double = 0.0
    var deliveryNoteReference: DeliveryNoteReference? = null
    var invoicedQuantity: Double = 0.0
    var itemEANCode: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var itemName: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var orderReference: OrderReference? = null
    var periodFromDate: Date? = null
    var periodToDate: Date? = null
    var unitPriceTaxAmount: Double = 0.0
    var unitPriceTaxExclusiveAmount: Double = 0.0

    val amount : Double get()  = invoicedQuantity + lineTaxAmount

    val unitPriceTaxInclusiveAmount: Double
        get() = unitPriceTaxExclusiveAmount + unitPriceTaxAmount

    val lineTaxAmount: Double
        get() = unitPriceTaxAmount * invoicedQuantity

    val lineTaxExclusiveAmount: Double
        get() = unitPriceTaxExclusiveAmount * invoicedQuantity

    val lineTaxInclusiveAmount: Double
        get() = unitPriceTaxInclusiveAmount * invoicedQuantity

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.nullOrVerify(orderReference)
        Verify.nullOrVerify(deliveryNoteReference)
        Verify.choice("ItemName ItemEANCode", arrayOf<Any?>(itemName, itemEANCode))
        Verify.periodFromToDate(periodFromDate, periodToDate)
        Verify.notNullAndPercentage("ClassifiedTaxCategory", classifiedTaxCategory)
    }
}
