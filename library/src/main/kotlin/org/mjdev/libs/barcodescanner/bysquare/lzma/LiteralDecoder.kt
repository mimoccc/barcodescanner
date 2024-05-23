package org.mjdev.libs.barcodescanner.bysquare.lzma

internal class LiteralDecoder {
    private lateinit var coders: Array<Decoder2?>

    private var numPrevBits = 0
    private var numPosBits = 0
    private var posMask = 0

    fun init() {
        val numStates = 1 shl numPrevBits + numPosBits
        for (i in 0 until numStates) {
            coders[i]?.init()
        }
    }

    fun create(numPosBits: Int, numPrevBits: Int) {
        if (this.numPrevBits == numPrevBits && this.numPosBits == numPosBits) {
            return
        }
        this.numPosBits = numPosBits
        posMask = (1 shl numPosBits) - 1
        this.numPrevBits = numPrevBits
        val numStates = 1 shl this.numPrevBits + this.numPosBits
        coders = arrayOfNulls(numStates)
        for (i in 0 until numStates) {
            coders[i] = Decoder2()
        }
    }

    fun getDecoder(pos: Int, prevByte: Byte): Decoder2? {
        return coders[(pos and posMask shl numPrevBits) + (prevByte.toInt()
            .and(0xFF) ushr 8 - numPrevBits)]
    }
}
