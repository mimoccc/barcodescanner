package org.mjdev.libs.barcodescanner.bysquare.data.invoiceitems

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class DeliveryNoteReference : IVerifiable {
    var deliveryNoteID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var deliveryNoteLineID: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    val isEmpty: Boolean
        get() = deliveryNoteID == null && deliveryNoteLineID == null

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
