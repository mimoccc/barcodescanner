package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import java.util.*

class BankAccounts : ArrayList<BankAccount?>(), IVerifiable {
    @Throws(InvalidValueException::class)
    override fun verify() {
    }
}
