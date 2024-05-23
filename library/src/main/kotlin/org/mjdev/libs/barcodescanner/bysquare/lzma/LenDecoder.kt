package org.mjdev.libs.barcodescanner.bysquare.lzma

import org.mjdev.libs.barcodescanner.bysquare.lzma.RangeDecoder.Companion.initBitModels
import java.io.IOException

internal class LenDecoder {
    private val choice = ShortArray(2)
    private val lowCoder = arrayOfNulls<BitTreeDecoder>(Base.kNumPosStatesMax)
    private val midCoder = arrayOfNulls<BitTreeDecoder>(Base.kNumPosStatesMax)
    private val highCoder = BitTreeDecoder(Base.kNumHighLenBits)
    private var numPosStates = 0
    
    fun create(numPosStates: Int) {
        while (this.numPosStates < numPosStates) {
            lowCoder[this.numPosStates] = BitTreeDecoder(Base.kNumLowLenBits)
            midCoder[this.numPosStates] = BitTreeDecoder(Base.kNumMidLenBits)
            this.numPosStates++
        }
    }

    fun init() {
        initBitModels(choice)
        for (posState in 0 until numPosStates) {
            lowCoder[posState]?.init()
            midCoder[posState]?.init()
        }
        highCoder.init()
    }

    @Throws(IOException::class)
    fun decode(rangeDecoder: RangeDecoder, posState: Int): Int {
        if (rangeDecoder.decodeBit(choice, 0) == 0) {
            return lowCoder[posState]?.decode(rangeDecoder) ?: 0
        }
        var symbol = Base.kNumLowLenSymbols
        symbol += if (rangeDecoder.decodeBit(choice, 1) == 0) {
            midCoder[posState]?.decode(rangeDecoder) ?: 0
        } else {
            Base.kNumMidLenSymbols + highCoder.decode(rangeDecoder)
        }
        return symbol
    }
}
