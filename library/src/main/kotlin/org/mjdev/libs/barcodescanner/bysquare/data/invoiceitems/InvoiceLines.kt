package org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import java.util.*

class InvoiceLines : ArrayList<InvoiceLine?>(), IVerifiable {
    val amount:Double get() = sumOf {
        it?.amount ?: 0.0
    }

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
