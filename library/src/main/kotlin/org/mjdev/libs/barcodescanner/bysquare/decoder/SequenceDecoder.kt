package org.mjdev.libs.barcodescanner.bysquare.decoder

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

abstract class SequenceDecoder internal constructor(fields: MutableList<String?>) {
    @SuppressLint("SimpleDateFormat")
    protected val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyyMMdd").apply {
        isLenient = false
    }
    private val fields: List<String?>
    private var index: Int

    init {
        this.fields = fields
        index = 0
        for (i in this.fields.indices) {
            if (fields[i]?.length == 0) {
                fields[i] = null
            }
        }
    }

    protected operator fun next(): String? {
        return if (index < fields.size) {
            val field = fields[index]
            index += 1
            field
        } else {
            null
        }
    }

    protected fun nextDate(): Date? {
        val string = nextString()
        if (string != null && string.length == 8) {
            try {
                return dateFormat.parse(string)
            } catch (t: Throwable) {
                // no op
            }
        }
        return null
    }

    protected fun nextDouble(): Double {
        val string = nextString()
        if (string != null) {
            try {
                return java.lang.Double.valueOf(string)
            } catch (t: Throwable) {
                // no op
            }
        }
        return 0.0
    }

    protected fun nextInt(): Int {
        val string = nextString()
        return if (string != null) {
            try {
                string.toInt()
            } catch (t: Throwable) {
                0
            }
        } else 0
    }

    protected fun nextInteger(): Int? {
        val string = nextString()
        return if (string != null) {
            try {
                Integer.valueOf(string)
            } catch (t: Throwable) {
                null
            }
        } else null
    }

    protected fun nextString(): String? {
        return next()
    }

}
