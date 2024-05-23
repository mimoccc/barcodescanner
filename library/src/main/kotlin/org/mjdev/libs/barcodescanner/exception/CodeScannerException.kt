package org.mjdev.libs.barcodescanner.exception

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Common class for code scanner runtime exceptions
 */
@Suppress("unused")
class CodeScannerException : RuntimeException {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)

    @RequiresApi(Build.VERSION_CODES.N)
    constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)
}
