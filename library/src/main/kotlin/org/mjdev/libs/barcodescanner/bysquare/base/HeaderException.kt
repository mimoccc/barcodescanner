package org.mjdev.libs.barcodescanner.bysquare.base

/**
 * Custom header exception
 */
@Suppress("unused")
class HeaderException : Exception {
    constructor()
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)
    constructor(t: Throwable?) : super(t)
}
