package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class TaxCategorySummary : IVerifiable {
    var alreadyClaimedTaxAmount: Double = 0.0
    var alreadyClaimedTaxExclusiveAmount: Double = 0.0
    var classifiedTaxCategory: Double = 0.0
    var taxAmount: Double = 0.0
    var taxExclusiveAmount: Double = 0.0

    val alreadyClaimedTaxInclusiveAmount: Double
        get() = alreadyClaimedTaxExclusiveAmount + alreadyClaimedTaxAmount

    val differenceTaxAmount: Double
        get() = taxAmount - alreadyClaimedTaxAmount

    val differenceTaxExclusiveAmount: Double
        get() = taxExclusiveAmount - alreadyClaimedTaxExclusiveAmount

    val differenceTaxInclusiveAmount: Double
        get() = taxInclusiveAmount - alreadyClaimedTaxInclusiveAmount

    val taxInclusiveAmount: Double
        get() = taxExclusiveAmount + taxAmount

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.notNullAndPercentage("ClassifiedTaxCategory", classifiedTaxCategory)
    }
}
