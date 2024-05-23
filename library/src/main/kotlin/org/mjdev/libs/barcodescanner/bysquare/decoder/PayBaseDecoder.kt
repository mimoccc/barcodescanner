package org.mjdev.libs.barcodescanner.bysquare.decoder

import org.mjdev.libs.barcodescanner.bysquare.data.PayBase
import org.mjdev.libs.barcodescanner.bysquare.data.pay.BankAccount
import org.mjdev.libs.barcodescanner.bysquare.data.pay.BankAccounts
import org.mjdev.libs.barcodescanner.bysquare.data.pay.DirectDebitExt
import org.mjdev.libs.barcodescanner.bysquare.data.pay.DirectDebitScheme
import org.mjdev.libs.barcodescanner.bysquare.data.pay.DirectDebitType
import org.mjdev.libs.barcodescanner.bysquare.data.pay.Month
import org.mjdev.libs.barcodescanner.bysquare.data.pay.Payment
import org.mjdev.libs.barcodescanner.bysquare.data.pay.PaymentOption
import org.mjdev.libs.barcodescanner.bysquare.data.pay.Payments
import org.mjdev.libs.barcodescanner.bysquare.data.pay.Periodicity
import org.mjdev.libs.barcodescanner.bysquare.data.pay.StandingOrderExt

class PayBaseDecoder(fields: List<String?>) : SequenceDecoder(fields.toMutableList()) {
    private fun decodeBankAccount(): BankAccount {
        return BankAccount().apply {
            iban = nextString()
            bic = nextString()
        }
    }

    private fun decodeBankAccounts(): BankAccounts {
        val bankAccounts = BankAccounts()
        var count = nextInt()
        while (true) {
            val position = count - 1
            if (count <= 0) {
                return bankAccounts
            }
            bankAccounts.add(decodeBankAccount())
            count = position
        }
    }

    private fun decodeBeneficiary(payments: Payments) {
        for (i in payments.indices) {
            payments[i].beneficiaryName = nextString()
            payments[i].beneficiaryAddressLine1 = nextString()
            payments[i].beneficiaryAddressLine2 = nextString()
        }
    }

    private fun decodeDirectDebitExt(): DirectDebitExt? {
        return if (nextInt() == 0) {
            null
        } else {
            DirectDebitExt().apply {
                directDebitScheme = DirectDebitScheme(nextInt())
                directDebitType = DirectDebitType(nextInt())
                variableSymbol = nextString()
                specificSymbol = nextString()
                originatorsReferenceInformation = nextString()
                mandateID = nextString()
                creditorID = nextString()
                contractID = nextString()
                maxAmount = nextDouble()
                validTillDate = nextDate()
            }
        }
    }

    private fun decodePayment(): Payment {
        return Payment().apply {
            paymentOptions = PaymentOption.choices(nextInt())
            amount = nextDouble()
            currencyCode = nextString()
            paymentDueDate = nextDate()
            variableSymbol = nextString()
            constantSymbol = nextString()
            specificSymbol = nextString()
            originatorsReferenceInformation = nextString()
            paymentNote = nextString()
            bankAccounts = decodeBankAccounts()
            standingOrderExt = decodeStandingOrderExt()
            directDebitExt = decodeDirectDebitExt()
        }
    }

    private fun decodePayments(): Payments {
        val payments = Payments()
        var count = nextInt()
        while (true) {
            val position = count - 1
            if (count <= 0) {
                decodeBeneficiary(payments)
                return payments
            }
            payments.add(decodePayment())
            count = position
        }
    }

    private fun decodeStandingOrderExt(): StandingOrderExt? {
        return if (nextInt() == 0) {
            null
        } else {
            StandingOrderExt().apply {
                day = nextInteger()
                month = Month.choices(nextInt())
                periodicity = Periodicity(nextString())
                lastDate = nextDate()
            }
        }
    }

    fun decode(payBase: PayBase) {
        payBase.invoiceId = nextString()
        payBase.payments = decodePayments()
    }
}
