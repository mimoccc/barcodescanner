package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceBase

@Suppress("unused", "MemberVisibilityCanBePrivate")
class MonetarySummary : IVerifiable {
    var invoice: InvoiceBase? = null
    var paidDepositsAmount: Double = 0.0
    var payableRoundingAmount: Double = 0.0

    val taxCategorySummaries: TaxCategorySummaries
        get() = invoice?.taxCategorySummaries ?: TaxCategorySummaries()

    val alreadyClaimedTaxAmount: Double
        get() {
            val iterator: MutableIterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().alreadyClaimedTaxAmount
            }
            return amount
        }

    val alreadyClaimedTaxExclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().alreadyClaimedTaxExclusiveAmount
            }
            return amount
        }

    val alreadyClaimedTaxInclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().alreadyClaimedTaxInclusiveAmount
            }
            return amount
        }

    val differenceTaxAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().differenceTaxAmount
            }
            return amount
        }

    val differenceTaxExclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().differenceTaxExclusiveAmount
            }
            return amount
        }

    val differenceTaxInclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().differenceTaxInclusiveAmount
            }
            return amount
        }

    val payableAmount: Double
        get() = differenceTaxInclusiveAmount - paidDepositsAmount + payableRoundingAmount

    val taxAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().taxAmount
            }
            return amount
        }

    val taxExclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().taxExclusiveAmount
            }
            return amount
        }

    val taxInclusiveAmount: Double
        get() {
            val iterator: Iterator<TaxCategorySummary> = taxCategorySummaries.iterator()
            var amount = 0.0
            while (iterator.hasNext()) {
                amount += iterator.next().taxInclusiveAmount
            }
            return amount
        }

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
