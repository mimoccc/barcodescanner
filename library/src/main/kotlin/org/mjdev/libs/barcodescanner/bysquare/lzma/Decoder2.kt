package org.mjdev.libs.barcodescanner.bysquare.lzma

import org.mjdev.libs.barcodescanner.bysquare.lzma.RangeDecoder.Companion.initBitModels
import java.io.IOException

internal class Decoder2 {
    private val decoders = ShortArray(0x300)

    fun init() {
        initBitModels(decoders)
    }

    @Throws(IOException::class)
    fun decodeNormal(rangeDecoder: RangeDecoder): Byte {
        var symbol = 1
        do {
            symbol = symbol shl 1 or rangeDecoder.decodeBit(decoders, symbol)
        } while (symbol < 0x100)
        return symbol.toByte()
    }

    @Throws(IOException::class)
    fun decodeWithMatchByte(rangeDecoder: RangeDecoder, mb: Byte): Byte {
        var matchByte = mb
        var symbol = 1
        do {
            val matchBit: Int = (matchByte.toInt() shr 7) and 1
            matchByte = (matchByte.toInt() shl 1).toByte()
            val bit = rangeDecoder.decodeBit(decoders, (1 + matchBit shl 8) + symbol)
            symbol = symbol shl 1 or bit
            if (matchBit != bit) {
                while (symbol < 0x100) {
                    symbol = symbol shl 1 or rangeDecoder.decodeBit(decoders, symbol)
                }
                break
            }
        } while (symbol < 0x100)
        return symbol.toByte()
    }
}
