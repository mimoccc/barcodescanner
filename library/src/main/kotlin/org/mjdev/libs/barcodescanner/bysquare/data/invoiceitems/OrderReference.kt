package org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class OrderReference : IVerifiable {
    var orderID: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    var orderLineID: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    val isEmpty: Boolean
        get() = orderID == null && orderLineID == null

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
