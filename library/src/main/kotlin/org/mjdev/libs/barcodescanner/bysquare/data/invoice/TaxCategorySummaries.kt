package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import java.util.*

class TaxCategorySummaries : ArrayList<TaxCategorySummary>(), IVerifiable {
    val tax: Double get() = firstOrNull()?.taxAmount ?: 0.0

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
