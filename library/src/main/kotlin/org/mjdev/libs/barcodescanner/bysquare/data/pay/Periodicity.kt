package org.mjdev.libs.barcodescanner.bysquare.data.pay

@Suppress("unused")
enum class Periodicity(
    val shortcut: String,
    val useDayMonth: Boolean
) {
    ANNUALLY("a", false),
    BIMONTHLY("B", true),
    BIWEEKLY("b", true),
    DAILY("d", false),
    MONTHLY("m", true),
    QUARTERLY("q", false),
    SEMIANNUALLY("s", false),
    WEEKLY("w", true);

    companion object {
        operator fun invoke(shortcut: String?) = entries.find { it.shortcut == shortcut }
    }
}
