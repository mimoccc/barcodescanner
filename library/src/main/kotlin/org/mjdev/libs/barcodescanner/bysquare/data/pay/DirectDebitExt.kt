package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify
import java.util.*

class DirectDebitExt : IVerifiable {
    var contractID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var creditorID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var directDebitScheme: DirectDebitScheme? = null
    var directDebitType: DirectDebitType? = null
    var mandateID: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var maxAmount: Double? = null
    var originatorsReferenceInformation: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var specificSymbol: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var validTillDate: Date? = null
    var variableSymbol: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.notNull("DirectDebitScheme", directDebitScheme)
        Verify.notNull("DirectDebitType", directDebitType)
        var message = "MandateID, CreditorID and ContractID are used"
        if (directDebitScheme != DirectDebitScheme.SEPA && mandateID == null &&
            creditorID == null && contractID == null
        ) {
            if (originatorsReferenceInformation != null) {
                Verify.isNull(
                    "VariableSymbol",
                    variableSymbol,
                    "OriginatorsReferenceInformation is used"
                )
                Verify.isNull(
                    "SpecificSymbol",
                    specificSymbol,
                    "OriginatorsReferenceInformation is used"
                )
            }
        } else {
            Verify.notNull("MandateID", mandateID)
            Verify.notNull("CreditorID", creditorID)
            if (directDebitScheme == DirectDebitScheme.SEPA) {
                message = "SEPA direct debit scheme selected"
            }
            Verify.isNull("VariableSymbol", variableSymbol, message)
            Verify.isNull("SpecificSymbol", specificSymbol, message)
            Verify.isNull(
                "OriginatorsReferenceInformation",
                originatorsReferenceInformation,
                message
            )
        }
        Verify.nullOrMatch("VariableSymbol", variableSymbol, Verify.REGEX_VARIABLE_SYMBOL)
        Verify.nullOrMatch("SpecificSymbol", specificSymbol, Verify.REGEX_SPECIFIC_SYMBOL)
        Verify.nullOrPositive("MaxAmount", maxAmount)
    }
}
