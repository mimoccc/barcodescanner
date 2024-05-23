package org.mjdev.libs.barcodescanner.bysquare.lzma

import java.io.IOException
import java.io.InputStream
import java.util.*

class RangeDecoder {
    private var range = 0
    private var code = 0
    private var stream: InputStream? = null
    
    fun setStream(stream: InputStream?) {
        this.stream = stream
    }

    fun releaseStream() {
        stream = null
    }

    @Throws(IOException::class)
    fun init() {
        code = 0
        range = -1
        for (i in 0..4) {
            code = code shl 8 or stream!!.read()
        }
    }

    @Throws(IOException::class)
    fun decodeDirectBits(numTotalBits: Int): Int {
        var result = 0
        for (i in numTotalBits downTo 1) {
            range = range ushr 1
            val t = code - range ushr 31
            code -= range and t - 1
            result = result shl 1 or 1 - t
            if (range and kTopMask == 0) {
                code = code shl 8 or stream!!.read()
                range = range shl 8
            }
        }
        return result
    }

    @Throws(IOException::class)
    fun decodeBit(probes: ShortArray, index: Int): Int {
        val prob = probes[index].toInt()
        val newBound = (range ushr kNumBitModelTotalBits) * prob
        return if (code xor -0x80000000 < newBound xor -0x80000000) {
            range = newBound
            probes[index] = (prob + (kBitModelTotal - prob ushr kNumMoveBits)).toShort()
            if (range and kTopMask == 0) {
                code = code shl 8 or stream!!.read()
                range = range shl 8
            }
            0
        } else {
            range -= newBound
            code -= newBound
            probes[index] = (prob - (prob ushr kNumMoveBits)).toShort()
            if (range and kTopMask == 0) {
                code = code shl 8 or stream!!.read()
                range = range shl 8
            }
            1
        }
    }

    companion object {
        private const val kTopMask = ((1 shl 24) - 1).inv()
        private const val kNumBitModelTotalBits = 11
        private const val kBitModelTotal = 1 shl kNumBitModelTotalBits
        private const val kNumMoveBits = 5
        
        @JvmStatic
        fun initBitModels(probes: ShortArray) {
            Arrays.fill(probes, (kBitModelTotal ushr 1).toShort())
        }
    }
}
