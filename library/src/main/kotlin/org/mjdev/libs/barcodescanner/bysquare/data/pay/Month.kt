package org.mjdev.libs.barcodescanner.bysquare.data.pay

@Suppress("unused")
enum class Month(val value: Int) {
    JANUARY(1),
    FEBRUARY(2),
    MARCH(4),
    APRIL(8),
    MAY(16),
    JUNE(32),
    JULY(64),
    AUGUST(128),
    SEPTEMBER(256),
    OCTOBER(512),
    NOVEMBER(1024),
    DECEMBER(2048);

    companion object {
        operator fun invoke(value: Int) = entries.find { it.value == value }

        fun choices(bitValue: Int): List<Month> {
            return ArrayList<Month>().apply {
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
