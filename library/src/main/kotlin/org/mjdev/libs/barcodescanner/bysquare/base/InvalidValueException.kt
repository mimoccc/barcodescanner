package org.mjdev.libs.barcodescanner.bysquare.base

/**
 * Invalid value exception
 */
@Suppress("unused")
class InvalidValueException : Exception {
    constructor()
    constructor(message: String?) : super(message)
    constructor(field: String?, message: String?) : super("$field : $message")
    constructor(message: String?, t: Throwable?) : super(message, t)
    constructor(t: Throwable?) : super(t)
}
