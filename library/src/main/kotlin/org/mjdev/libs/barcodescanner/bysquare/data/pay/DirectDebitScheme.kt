package org.mjdev.libs.barcodescanner.bysquare.data.pay

@Suppress("unused")
enum class DirectDebitScheme(val value: Int) {
    OTHER(0),
    SEPA(1);

    companion object {
        operator fun invoke(value: Int) = entries.find { it.value == value }
    }
}
