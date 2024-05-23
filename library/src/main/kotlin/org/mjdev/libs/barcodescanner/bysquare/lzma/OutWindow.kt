package org.mjdev.libs.barcodescanner.bysquare.lzma

import java.io.IOException
import java.io.OutputStream

class OutWindow {
    private var buffer: ByteArray? = null
    private var pos = 0
    private var windowSize = 0
    private var streamPos = 0
    private var stream: OutputStream? = null

    fun create(windowSize: Int) {
        if (buffer == null || this.windowSize != windowSize) {
            buffer = ByteArray(windowSize)
        }
        this.windowSize = windowSize
        pos = 0
        streamPos = 0
    }

    @Throws(IOException::class)
    fun setStream(stream: OutputStream?) {
        releaseStream()
        this.stream = stream
    }

    @Throws(IOException::class)
    fun releaseStream() {
        flush()
        stream = null
    }

    fun init(solid: Boolean) {
        if (!solid) {
            streamPos = 0
            pos = 0
        }
    }

    @Throws(IOException::class)
    fun flush() {
        val size = pos - streamPos
        if (size == 0) {
            return
        }
        stream?.write(buffer, streamPos, size)
        if (pos >= windowSize) {
            pos = 0
        }
        streamPos = pos
    }

    @Suppress("SpellCheckingInspection")
    @Throws(IOException::class)
    fun copyBlock(distance: Int, len: Int) {
        var clen = len
        var pos = pos - distance - 1
        if (pos < 0) {
            pos += windowSize
        }
        while (clen != 0) {
            if (pos >= windowSize) {
                pos = 0
            }
            buffer?.set(this.pos++, buffer?.get(pos++) ?: 0)
            if (this.pos >= windowSize) {
                flush()
            }
            clen--
        }
    }

    @Throws(IOException::class)
    fun putByte(b: Byte) {
        buffer?.set(pos++, b)
        if (pos >= windowSize) {
            flush()
        }
    }

    fun getByte(distance: Int): Byte {
        var pos = pos - distance - 1
        if (pos < 0) {
            pos += windowSize
        }
        return buffer?.get(pos) ?: 0
    }
}
