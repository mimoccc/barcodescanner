package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class BankAccount : IVerifiable {
    var bic: String? = null
        set(value) {
            field = Verify.trimAllToUpperCase(value)
        }
    var iban: String? = null
        set(value) {
            field = Verify.trimAllToUpperCase(value)
        }

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.notNullAndMatch("IBAN", iban, Verify.REGEX_IBAN)
        if (bic != null) {
            Verify.notNullAndMatch("BIC", bic, Verify.REGEX_BIC)
        }
    }
}
