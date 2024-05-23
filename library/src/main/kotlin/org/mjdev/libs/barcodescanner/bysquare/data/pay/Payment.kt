package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify
import java.util.*

class Payment : IVerifiable {
    var amount: Double = 0.0
    var bankAccounts: BankAccounts = BankAccounts()
    var beneficiaryAddressLine1: String? = null
    var beneficiaryAddressLine2: String? = null
    var beneficiaryName: String? = null
    var constantSymbol: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var currencyCode: String? = null
        set(value) {
            field = Verify.trimAllToUpperCase(value)
        }
    var directDebitExt: DirectDebitExt? = null
    var originatorsReferenceInformation: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var paymentDueDate: Date? = null
    var paymentNote: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var paymentOptions: List<PaymentOption>? = null
    var specificSymbol: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var standingOrderExt: StandingOrderExt? = null
    var variableSymbol: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.enumList("PaymentOptions", paymentOptions)
        Verify.notNullAndMatch("CurrencyCode", currencyCode, Verify.REGEX_CURRENCY_CODE)
        if (originatorsReferenceInformation != null) {
            Verify.isNull(
                "VariableSymbol",
                variableSymbol,
                "OriginatorsReferenceInformation is used"
            )
            Verify.isNull(
                "ConstantSymbol",
                constantSymbol,
                "OriginatorsReferenceInformation is used"
            )
            Verify.isNull(
                "SpecificSymbol",
                specificSymbol,
                "OriginatorsReferenceInformation is used"
            )
        } else {
            Verify.nullOrMatch("VariableSymbol", variableSymbol, Verify.REGEX_VARIABLE_SYMBOL)
            Verify.nullOrMatch("ConstantSymbol", constantSymbol, Verify.REGEX_CONSTANT_SYMBOL)
            Verify.nullOrMatch("SpecificSymbol", specificSymbol, Verify.REGEX_SPECIFIC_SYMBOL)
        }
        Verify.notNullAndVerify("BankAccounts", bankAccounts)
        if (paymentOptions!!.contains(PaymentOption.STANDING_ORDER)) {
            Verify.notNullAndVerify("StandingOrderExt", standingOrderExt)
        } else {
            val var1 = standingOrderExt
            Verify.isNull(
                "StandingOrderExt",
                var1,
                "List of PaymentOptions does not contain StandingOrder"
            )
        }
        if (paymentOptions!!.contains(PaymentOption.DIRECT_DEBIT)) {
            Verify.notNullAndVerify("DirectDebitExt", directDebitExt)
        } else {
            val directDebitExt = directDebitExt
            Verify.isNull(
                "DirectDebitExt",
                directDebitExt,
                "List of PaymentOptions does not contain DirectDebit."
            )
        }
    }
}
