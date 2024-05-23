package org.mjdev.libs.barcodescanner.bysquare.data.pay

@Suppress("unused")
enum class PaymentOption(val value: Int) {
    PAYMENT_ORDER(1),
    STANDING_ORDER(2),
    DIRECT_DEBIT(4);

    companion object {
        operator fun invoke(value: Int) = entries.find { it.value == value }

        fun choices(bitValue: Int): List<PaymentOption> {
            return ArrayList<PaymentOption>().apply {
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
