package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import java.util.*

class Payments : ArrayList<Payment>(), IVerifiable {
    @get:Throws(InvalidValueException::class)
    val amount: Double
        get() {
            var accumulated = 0.0
            if (size > 0) {
                val iterator: Iterator<Payment> = iterator()
                val currency = get(0).currencyCode
                while (iterator.hasNext()) {
                    val payment = iterator.next()
                    if (payment.currencyCode == currency) {
                        accumulated += payment.amount
                    } else {
                        throw InvalidValueException("Payments have differences in currencies.")
                    }
                }
            }
            return accumulated
        }

    val currency: String?
        get() = if (size == 1) get(0).currencyCode else null

    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
