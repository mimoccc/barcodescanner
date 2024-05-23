package org.mjdev.libs.barcodescanner.bysquare.data.invoice

@Suppress("unused")
enum class PaymentMean(val value: Int) {
    OTHER(0),
    CASH(1),
    CASH_ON_DELIVERY(2),
    MONEY_TRANSFER(4),
    CREDIT_CARD(8),
    MUTUAL_OFFSET(16),
    ADVANCE(32);

    companion object {
        operator fun invoke(value: Int) = entries.find { it.value == value }

        fun choices(bitValue: Int): List<PaymentMean> {
            return ArrayList<PaymentMean>().apply {
                val enumList = entries.toTypedArray()
                val count = enumList.size
                for (i in 0 until count) {
                    val e = enumList[i]
                    if (((1 shl bitValue) and e.value) != 0) add(e)
                }
            }
        }
    }
}
