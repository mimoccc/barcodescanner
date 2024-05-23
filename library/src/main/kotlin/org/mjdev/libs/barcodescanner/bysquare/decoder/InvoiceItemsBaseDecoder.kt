package org.mjdev.libs.barcodescanner.bysquare.decoder

import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceItemsBase
import org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems.DeliveryNoteReference
import org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems.InvoiceLine
import org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems.InvoiceLines
import org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems.OrderReference

class InvoiceItemsBaseDecoder(fields: List<String?>) : SequenceDecoder(fields.toMutableList()) {
    private fun decodeDeliveryNoteReference(): DeliveryNoteReference? {
        return DeliveryNoteReference().apply {
            deliveryNoteID = nextString()
            deliveryNoteLineID = nextString()
        }.let { deliveryNoteReference ->
            if (deliveryNoteReference.isEmpty) null
            else deliveryNoteReference
        }
    }

    private fun decodeInvoiceLine(): InvoiceLine {
        return InvoiceLine().apply {
            orderReference = decodeOrderReference()
            deliveryNoteReference = decodeDeliveryNoteReference()
            itemName = nextString()
            itemEANCode = nextString()
            periodFromDate = nextDate()
            periodToDate = nextDate()
            invoicedQuantity = nextDouble()
            unitPriceTaxExclusiveAmount = nextDouble()
            unitPriceTaxAmount = nextDouble()
            classifiedTaxCategory = nextDouble()
        }
    }

    private fun decodeInvoiceLines(): InvoiceLines {
        val invoiceLines = InvoiceLines()
        var count = nextInt()
        while (true) {
            val position = count - 1
            if (count <= 0) {
                return invoiceLines
            }
            invoiceLines.add(decodeInvoiceLine())
            count = position
        }
    }

    private fun decodeOrderReference(): OrderReference? {
        return OrderReference().apply {
            orderID = nextString()
            orderLineID = nextString()
        }.let { orderReference ->
            if (orderReference.isEmpty) null
            else orderReference
        }
    }

    fun decode(invoiceItemsBase: InvoiceItemsBase) {
        invoiceItemsBase.invoiceId = nextString()
        invoiceItemsBase.firstInvoiceLineID = nextString()
        invoiceItemsBase.invoiceLines = decodeInvoiceLines()
    }
}
