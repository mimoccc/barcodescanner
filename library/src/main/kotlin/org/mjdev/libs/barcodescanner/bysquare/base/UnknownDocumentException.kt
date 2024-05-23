package org.mjdev.libs.barcodescanner.bysquare.base

/**
 * Custom document exception
 */
@Suppress("unused")
class UnknownDocumentException : Exception {
    constructor()
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)
    constructor(t: Throwable?) : super(t)
}
