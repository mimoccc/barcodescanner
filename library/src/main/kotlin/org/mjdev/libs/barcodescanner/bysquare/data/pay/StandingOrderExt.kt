package org.mjdev.libs.barcodescanner.bysquare.data.pay

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify
import java.util.*

class StandingOrderExt : IVerifiable {
    var day: Int? = null
    var lastDate: Date? = null
    var month: List<Month>? = null
    var periodicity: Periodicity? = null

    @Throws(InvalidValueException::class)
    override fun verify() {
        val periodicity = periodicity
        val day = day
        val month = month
        Verify.notNull("Periodicity", periodicity)
        if (!periodicity!!.useDayMonth) {
            val message = "periodicity ${
                periodicity.toString().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                    else it.toString()
                }
            } does not allow Day or Month to be specified."
            Verify.isNull("Day", day, message)
            Verify.isNull("Month", month, message)
        } else {
            val field: String
            val value: Byte
            if (periodicity !== Periodicity.WEEKLY && periodicity !== Periodicity.BIWEEKLY) {
                field = "Day"
                value = 31
            } else {
                field = "Day"
                value = 7
            }
            Verify.nullOrInRange(field, day, 1, value.toInt())
        }
        if (month != null) {
            Verify.enumList("Month", month)
        }
    }
}
