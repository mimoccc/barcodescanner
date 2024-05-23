package org.mjdev.libs.barcodescanner.bysquare.data.pay

@Suppress("unused")
enum class DirectDebitType(val value: Int) {
    ONE_OFF(0),
    RECURRENT(1);

    override fun toString(): String {
        return if (this == ONE_OFF) "one-off" else super.toString()
    }

    companion object {
        operator fun invoke(value: Int) = entries.find { it.value == value }
    }
}
