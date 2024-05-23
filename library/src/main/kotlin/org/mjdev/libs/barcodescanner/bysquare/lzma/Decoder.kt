package org.mjdev.libs.barcodescanner.bysquare.lzma

import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.getLenToPosState
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateInit
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateIsCharState
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateUpdateChar
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateUpdateMatch
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateUpdateRep
import org.mjdev.libs.barcodescanner.bysquare.lzma.Base.stateUpdateShortRep
import org.mjdev.libs.barcodescanner.bysquare.lzma.BitTreeDecoder.Companion.reverseDecode
import org.mjdev.libs.barcodescanner.bysquare.lzma.RangeDecoder.Companion.initBitModels
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max

class Decoder {
    private val outWindow = OutWindow()
    private val rangeDecoder = RangeDecoder()
    private val isMatchDecoders = ShortArray(Base.kNumStates shl Base.kNumPosStatesBitsMax)
    private val isRepDecoders = ShortArray(Base.kNumStates)
    private val isRepG0Decoders = ShortArray(Base.kNumStates)
    private val isRepG1Decoders = ShortArray(Base.kNumStates)
    private val isRepG2Decoders = ShortArray(Base.kNumStates)
    private val isRep0LongDecoders = ShortArray(Base.kNumStates shl Base.kNumPosStatesBitsMax)
    private val posSlotDecoder = arrayOfNulls<BitTreeDecoder>(Base.kNumLenToPosStates)
    private val posDecoders = ShortArray(Base.kNumFullDistances - Base.kEndPosModelIndex)
    private val posAlignDecoder = BitTreeDecoder(Base.kNumAlignBits)
    private val lenDecoder = LenDecoder()
    private val repLenDecoder = LenDecoder()
    private val literalDecoder = LiteralDecoder()
    private var dictionarySize = -1
    private var dictionarySizeCheck = -1
    private var posStateMask = 0

    init {
        for (i in 0 until Base.kNumLenToPosStates) {
            posSlotDecoder[i] = BitTreeDecoder(Base.kNumPosSlotBits)
        }
    }
    
    fun setDictionarySize(dictionarySize: Int): Boolean {
        if (dictionarySize < 0) {
            return false
        }
        if (this.dictionarySize != dictionarySize) {
            this.dictionarySize = dictionarySize
            dictionarySizeCheck = max(this.dictionarySize, 1)
            outWindow.create(max(dictionarySizeCheck, 1 shl 12))
        }
        return true
    }

    fun setLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
        if (lc > Base.kNumLitContextBitsMax || lp > 4 || pb > Base.kNumPosStatesBitsMax) {
            return false
        }
        literalDecoder.create(lp, lc)
        val numPosStates = 1 shl pb
        lenDecoder.create(numPosStates)
        repLenDecoder.create(numPosStates)
        posStateMask = numPosStates - 1
        return true
    }

    @Throws(IOException::class)
    fun init() {
        outWindow.init(false)
        initBitModels(isMatchDecoders)
        initBitModels(isRep0LongDecoders)
        initBitModels(isRepDecoders)
        initBitModels(isRepG0Decoders)
        initBitModels(isRepG1Decoders)
        initBitModels(isRepG2Decoders)
        initBitModels(posDecoders)
        literalDecoder.init()
        var i = 0
        while (i < Base.kNumLenToPosStates) {
            posSlotDecoder[i]?.init()
            i++
        }
        lenDecoder.init()
        repLenDecoder.init()
        posAlignDecoder.init()
        rangeDecoder.init()
    }

    @Throws(IOException::class)
    fun code(inStream: InputStream, outStream: OutputStream, outSize: Long): Boolean {
        rangeDecoder.setStream(inStream)
        outWindow.setStream(outStream)
        init()
        var state = stateInit()
        var rep0 = 0
        var rep1 = 0
        var rep2 = 0
        var rep3 = 0
        var nowPos64: Long = 0
        var prevByte: Byte = 0
        while (outSize < 0 || nowPos64 < outSize) {
            val posState = nowPos64.toInt() and posStateMask
            if (rangeDecoder.decodeBit(
                    isMatchDecoders,
                    (state shl Base.kNumPosStatesBitsMax) + posState
                ) == 0
            ) {
                val decoder2 = literalDecoder.getDecoder(nowPos64.toInt(), prevByte)
                prevByte = if (!stateIsCharState(state)) {
                    decoder2?.decodeWithMatchByte(rangeDecoder, outWindow.getByte(rep0))
                } else {
                    decoder2?.decodeNormal(rangeDecoder)
                } ?: 0
                outWindow.putByte(prevByte)
                state = stateUpdateChar(state)
                nowPos64++
            } else {
                var len: Int
                if (rangeDecoder.decodeBit(isRepDecoders, state) == 1) {
                    len = 0
                    if (rangeDecoder.decodeBit(isRepG0Decoders, state) == 0) {
                        if (rangeDecoder.decodeBit(
                                isRep0LongDecoders,
                                (state shl Base.kNumPosStatesBitsMax) + posState
                            ) == 0
                        ) {
                            state = stateUpdateShortRep(state)
                            len = 1
                        }
                    } else {
                        var distance: Int
                        if (rangeDecoder.decodeBit(isRepG1Decoders, state) == 0) {
                            distance = rep1
                        } else {
                            if (rangeDecoder.decodeBit(isRepG2Decoders, state) == 0) {
                                distance = rep2
                            } else {
                                distance = rep3
                                rep3 = rep2
                            }
                            rep2 = rep1
                        }
                        rep1 = rep0
                        rep0 = distance
                    }
                    if (len == 0) {
                        len = repLenDecoder.decode(rangeDecoder, posState) + Base.kMatchMinLen
                        state = stateUpdateRep(state)
                    }
                } else {
                    rep3 = rep2
                    rep2 = rep1
                    rep1 = rep0
                    len = Base.kMatchMinLen + lenDecoder.decode(rangeDecoder, posState)
                    state = stateUpdateMatch(state)
                    val posSlot = posSlotDecoder[getLenToPosState(len)]!!
                        .decode(rangeDecoder)
                    if (posSlot >= Base.kStartPosModelIndex) {
                        val numDirectBits = (posSlot shr 1) - 1
                        rep0 = 2 or (posSlot and 1) shl numDirectBits
                        if (posSlot < Base.kEndPosModelIndex) {
                            rep0 += reverseDecode(
                                posDecoders,
                                rep0 - posSlot - 1,
                                rangeDecoder,
                                numDirectBits
                            )
                        } else {
                            rep0 += rangeDecoder.decodeDirectBits(numDirectBits - Base.kNumAlignBits) shl Base.kNumAlignBits
                            rep0 += posAlignDecoder.reverseDecode(rangeDecoder)
                            if (rep0 < 0) {
                                if (rep0 == -1) {
                                    break
                                }
                                return false
                            }
                        }
                    } else {
                        rep0 = posSlot
                    }
                }
                if (rep0 >= nowPos64 || rep0 >= dictionarySizeCheck) {
                    return false
                }
                outWindow.copyBlock(rep0, len)
                nowPos64 += len.toLong()
                prevByte = outWindow.getByte(0)
            }
        }
        outWindow.flush()
        outWindow.releaseStream()
        rangeDecoder.releaseStream()
        return true
    }
}
