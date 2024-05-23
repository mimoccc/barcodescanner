package org.mjdev.libs.barcodescanner.bysquare.decoder

import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceBase
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.Contact
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.CustomerParty
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.MonetarySummary
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.Party
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.PaymentMean
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.PostalAddress
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.SingleInvoiceLine
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.SupplierParty
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.TaxCategorySummaries
import org.mjdev.libs.barcodescanner.bysquare.data.invoice.TaxCategorySummary

class InvoiceBaseDecoder(fields: List<String?>) : SequenceDecoder(fields.toMutableList()) {
    private fun decodeContact(): Contact? {
        return Contact().apply {
            name = nextString()
            telephone = nextString()
            email = nextString()
        }.let { contact ->
            if (contact.isEmpty) null
            else contact
        }
    }

    private fun decodeCustomerParty(): CustomerParty {
        return CustomerParty().apply {
            decodeParty(this)
            partyIdentification = nextString()
        }
    }

    private fun decodeSupplierParty(): SupplierParty {
        return SupplierParty().apply {
            decodeParty(this)
            postalAddress = decodePostalAddress()
            contact = decodeContact()
        }
    }

    private fun decodeTaxCategorySummaries(): TaxCategorySummaries {
        val taxCategorySummaries = TaxCategorySummaries()
        var count = nextInt()
        while (true) {
            val position = count - 1
            if (count <= 0) {
                return taxCategorySummaries
            }
            taxCategorySummaries.add(decodeTaxCategorySummary())
            count = position
        }
    }

    private fun decodeMonetarySummary(): MonetarySummary {
        return MonetarySummary().apply {
            payableRoundingAmount = nextDouble()
            paidDepositsAmount = nextDouble()
        }
    }

    private fun decodeParty(party: Party) {
        with(party) {
            partyName = nextString()
            companyTaxID = nextString()
            companyVATID = nextString()
            companyRegisterID = nextString()
        }
    }

    private fun decodePostalAddress(): PostalAddress {
        return PostalAddress().apply {
            streetName = nextString()
            buildingNumber = nextString()
            cityName = nextString()
            postalZone = nextString()
            state = nextString()
            country = nextString()
        }
    }

    private fun decodeSingleInvoiceLine(invoice: InvoiceBase): SingleInvoiceLine? {
        return SingleInvoiceLine().apply {
            this.invoice = invoice
            orderLineID = nextString()
            deliveryNoteLineID = nextString()
            itemName = nextString()
            itemEANCode = nextString()
            periodFromDate = nextDate()
            periodToDate = nextDate()
            invoicedQuantity = nextDouble()
        }.let { singleInvoiceLine ->
            if (singleInvoiceLine.isEmpty) null
            else singleInvoiceLine
        }
    }

    private fun decodeTaxCategorySummary(): TaxCategorySummary {
        return TaxCategorySummary().apply {
            classifiedTaxCategory = nextDouble()
            taxExclusiveAmount = nextDouble()
            taxAmount = nextDouble()
            alreadyClaimedTaxExclusiveAmount = nextDouble()
            alreadyClaimedTaxAmount = nextDouble()
        }
    }

    fun decode(invoiceBase: InvoiceBase) {
        invoiceBase.invoiceId = nextString()
        invoiceBase.issueDate = nextDate()
        invoiceBase.taxPointDate = nextDate()
        invoiceBase.orderID = nextString()
        invoiceBase.deliveryNoteID = nextString()
        invoiceBase.localCurrencyCode = nextString()
        invoiceBase.foreignCurrencyCode = nextString()
        invoiceBase.currRate = nextDouble()
        invoiceBase.referenceCurrRate = nextDouble()
        invoiceBase.supplierParty = decodeSupplierParty()
        invoiceBase.customerParty = decodeCustomerParty()
        invoiceBase.numberOfInvoiceLines = nextInteger()
        invoiceBase.invoiceDescription = nextString()
        invoiceBase.singleInvoiceLine = decodeSingleInvoiceLine(invoiceBase)
        invoiceBase.taxCategorySummaries = decodeTaxCategorySummaries()
        invoiceBase.monetarySummary = decodeMonetarySummary()
        invoiceBase.paymentMeans = PaymentMean.choices(nextInt())
    }
}
