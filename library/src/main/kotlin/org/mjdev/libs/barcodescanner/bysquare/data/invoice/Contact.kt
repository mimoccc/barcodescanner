package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class Contact : IVerifiable {
    var email: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    var name: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    var telephone: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    val isEmpty: Boolean get() = name == null && email == null && telephone == null

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
