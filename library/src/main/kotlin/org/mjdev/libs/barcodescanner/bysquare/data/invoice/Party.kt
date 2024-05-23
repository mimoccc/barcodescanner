package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

open class Party : IVerifiable {
    var companyRegisterID: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    var companyTaxID: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    var companyVATID: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    var partyName: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    val isEmpty: Boolean
        get() = partyName == null && companyTaxID == null && companyVATID == null &&
                companyRegisterID == null

    override fun toString(): String {
        return partyName ?: ""
    }

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.notNull("PartyName", partyName)
    }
}
