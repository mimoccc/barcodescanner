package org.mjdev.libs.barcodescanner.bysquare.base

interface IVerifiable {
    @Throws(InvalidValueException::class)
    fun verify()
}
