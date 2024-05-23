package org.mjdev.libs.barcodescanner.bysquare.base

import org.mjdev.libs.barcodescanner.extensions.hasDuplicates
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object Verify {
    private const val NOT_POSITIVE = "cannot be negative, but value %s found"

    const val REGEX_BIC = "[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?"
    const val REGEX_IBAN = "[A-Z]{2}[0-9]{2}[A-Z0-9]{0,30}"
    const val REGEX_CONSTANT_SYMBOL = "[0-9]{0,4}"
    const val REGEX_COUNTRY_CODE = "[A-Z]{3}"
    const val REGEX_CURRENCY_CODE = "[A-Z]{3}"
    const val REGEX_SPECIFIC_SYMBOL = "[0-9]{0,10}"
    const val REGEX_VARIABLE_SYMBOL = "[0-9]{0,10}"

    private val matcher: ThreadLocal<*> = object : ThreadLocal<Any?>() {
        override fun initialValue(): Matcher? {
            return Pattern.compile("\\s").matcher("")
        }
    }

    @Throws(InvalidValueException::class)
    fun choice(choicesString: String, vararg values: Any?) {
        val choices = choicesString.split(" ").toTypedArray()
        var prevValue: String? = null
        // for each in choices
        for (i in values.indices) {
            if (values[i] != null) {
                val choice = choices[i]
                // check if set more than one
                if (prevValue != null) {
                    throw InvalidValueException("$choice conflicts with $prevValue field value.")
                }
                // else one set
                prevValue = choice
            }
        }
        // no value from choices
        if (prevValue == null) {
            throw InvalidValueException("At least one field is required from: $choicesString")
        }
    }

    @Throws(InvalidValueException::class)
    fun isNull(field: String?, value: Any?, detail: String) {
        if (value != null) throw InvalidValueException(field, "must be null, $detail")
    }

    @Throws(InvalidValueException::class)
    fun notNull(field: String, value: Any?) {
        if (value == null) throw InvalidValueException("$field cannot be null.")
    }

    @Throws(InvalidValueException::class)
    fun notNullAndMatch(field: String, value: String?, match: String) {
        notNull(field, value)
        nullOrMatch(field, value, match)
    }

    @Throws(InvalidValueException::class)
    fun notNullAndPercentage(field: String, value: Double) {
        notNull(field, value)
        if (value < 0.0 || value > 1.0)
            throw InvalidValueException("$field is invalid, value must be between 0 and 1 inclusive.")
    }

    @Throws(InvalidValueException::class)
    fun notNullAndVerify(field: String, value: IVerifiable?) {
        notNull(field, value)
        value?.verify()
    }

    @Throws(InvalidValueException::class)
    fun nullOrInRange(field: String?, value: Int?, rangeFrom: Int, rangeTo: Int) {
        if (value != null) {
            if (value < rangeFrom || value > rangeTo) {
                throw InvalidValueException(
                    field, String.format("%s is out of range [%s..%s]", value, rangeFrom, rangeTo)
                )
            }
        }
    }

    @Throws(InvalidValueException::class)
    fun nullOrMatch(field: String?, value: String?, match: String) {
        if (value != null) {
            if (!value.matches(match.toRegex()))
                throw InvalidValueException("$field value does not match $match.")
        }
    }

    @Throws(InvalidValueException::class)
    fun nullOrPositive(field: String?, value: Double?) {
        if (value != null) {
            if (value < 0.0) throw InvalidValueException(field, String.format(NOT_POSITIVE, value))
        }
    }

    @Throws(InvalidValueException::class)
    fun nullOrVerify(value: IVerifiable?) {
        value?.verify()
    }

    @Throws(InvalidValueException::class)
    fun periodFromToDate(fromDate: Date?, toDate: Date?) {
        if (fromDate != null || toDate != null) {
            notNull("PeriodFromDate", fromDate)
            notNull("PeriodToDate", toDate)
            if (fromDate!!.after(toDate)) {
                throw InvalidValueException("PeriodFromDate is after PeriodToDate.")
            }
        }
    }

    fun trim(value: String?): String? {
        return if (value != null) {
            val value1 = value.trim { it <= ' ' }
            if (value.isEmpty()) null
            else value1
        } else null
    }

    fun trimAll(value: String?): String? {
        return if (value != null) {
            val value1 = (matcher.get() as Matcher).reset(value).replaceAll("")
            value1.ifEmpty { null }
        } else null
    }

    fun trimAllToUpperCase(value: String?): String? {
        return if (value != null) {
            val value1 = (matcher.get() as Matcher).reset(value).replaceAll("")
            if (value1.isEmpty()) null
            else value1.uppercase(Locale.getDefault())
        } else null
    }

    fun <T : Enum<*>> enumList(field: String, items: List<T>?) {
        if (items?.hasDuplicates() == true) {
            throw InvalidValueException(field, "has duplicate choices in list.")
        }
    }
}
