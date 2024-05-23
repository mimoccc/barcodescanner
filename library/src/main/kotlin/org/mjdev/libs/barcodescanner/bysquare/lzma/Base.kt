package org.mjdev.libs.barcodescanner.bysquare.lzma

@Suppress("MemberVisibilityCanBePrivate")
object Base {
    const val kNumStates = 12
    const val kNumAlignBits = 4
    const val kStartPosModelIndex = 4
    const val kEndPosModelIndex = 14
    const val kNumFullDistances = 1 shl kEndPosModelIndex / 2
    const val kNumLitContextBitsMax = 8
    const val kNumPosStatesBitsMax = 4
    const val kNumPosStatesMax = 1 shl kNumPosStatesBitsMax
    const val kNumLowLenBits = 3
    const val kNumMidLenBits = 3
    const val kNumHighLenBits = 8
    const val kNumLowLenSymbols = 1 shl kNumLowLenBits
    const val kNumMidLenSymbols = 1 shl kNumMidLenBits
    const val kNumPosSlotBits = 6
    const val kNumLenToPosStatesBits = 2
    const val kNumLenToPosStates = 1 shl kNumLenToPosStatesBits
    const val kMatchMinLen = 2

    fun stateInit(): Int {
        return 0
    }

    fun stateUpdateChar(index: Int): Int {
        if (index < 4) return 0
        return if (index < 10) index - 3
        else index - 6
    }

    fun stateUpdateMatch(index: Int): Int {
        return if (index < 7) 7 else 10
    }

    fun stateUpdateRep(index: Int): Int {
        return if (index < 7) 8 else 11
    }

    fun stateUpdateShortRep(index: Int): Int {
        return if (index < 7) 9 else 11
    }

    fun stateIsCharState(index: Int): Boolean {
        return index < 7
    }

    @Suppress("SpellCheckingInspection")
    fun getLenToPosState(len: Int): Int {
        val clen = len - kMatchMinLen
        return if (clen < kNumLenToPosStates) clen else kNumLenToPosStates - 1
    }
}
