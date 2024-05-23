package org.mjdev.libs.barcodescanner.bysquare.lzma

import java.io.IOException

class BitTreeDecoder(private val numBitLevels: Int) {
    private val models: ShortArray = ShortArray(1 shl numBitLevels)

    fun init() {
        RangeDecoder.initBitModels(models)
    }

    @Throws(IOException::class)
    fun decode(rangeDecoder: RangeDecoder): Int {
        var m = 1
        for (bitIndex in numBitLevels downTo 1) {
            m = (m shl 1) + rangeDecoder.decodeBit(models, m)
        }
        return m - (1 shl numBitLevels)
    }

    @Throws(IOException::class)
    fun reverseDecode(rangeDecoder: RangeDecoder): Int {
        var m = 1
        var symbol = 0
        for (bitIndex in 0 until numBitLevels) {
            val bit = rangeDecoder.decodeBit(models, m)
            m = m shl 1
            m += bit
            symbol = symbol or (bit shl bitIndex)
        }
        return symbol
    }

    companion object {
        @Throws(IOException::class)
        fun reverseDecode(
            models: ShortArray,
            startIndex: Int,
            rangeDecoder: RangeDecoder,
            numBitLevels: Int
        ): Int {
            var m = 1
            var symbol = 0
            for (bitIndex in 0 until numBitLevels) {
                val bit = rangeDecoder.decodeBit(models, startIndex + m)
                m = m shl 1
                m += bit
                symbol = symbol or (bit shl bitIndex)
            }
            return symbol
        }
    }
}
