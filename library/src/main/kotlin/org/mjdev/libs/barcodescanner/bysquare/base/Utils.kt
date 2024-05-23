package org.mjdev.libs.barcodescanner.bysquare.base

object Utils {
    /**
     * Copy one array of bytes to another array of bytes
     * Starting at position srcPos, and with length len
     * @param bytes source data
     * @param srcPos start position to copy data at
     * @param len size of copied packet
     * @return new array of bytes
     */
    @JvmStatic
    fun copyOfRange(bytes: ByteArray, srcPos: Int, len: Int): ByteArray {
        val dest = ByteArray(len)
        System.arraycopy(bytes, srcPos, dest, 0, len)
        return dest
    }

    /**
     * Copy bytes from one array to new
     * @param bytes data to copy from
     * @param size size of new array with copied data
     */
    @JvmStatic
    fun copyOf(bytes: ByteArray, size: Int): ByteArray {
        return copyOfRange(bytes, 0, size)
    }

    /**
     * Converts 5 bits sequence in Base32Hex code to byte array
     * First made 5 bits chunks from base32hex chars
     * Next join it to string
     * Parse binary string to 8bit chunks and made bytearray
     * @param text, text to decode from base32hex
     */
    @JvmStatic
    fun decodeBase32HexString(text: String): ByteArray {
        return text.toCharArray().joinToString("") { char ->
            if (char in '0'..'9') {
                Integer.toBinaryString(char.code - '0'.code)
            } else {
                Integer.toBinaryString(char.code - 'A'.code + 10)
            }.padStart(5, '0')
        }.let { bitString ->
            splitEqually(bitString, 8)
        }.map { bits ->
            Integer.parseInt(bits, 2).toByte()
        }.toByteArray()
    }

    /**
     * Split string to array of strings with equal size
     * Used for to transform 5bit array to 8bit array
     * @param text text to parse
     * @param size, size of text chunk, to be parsed
     * @return list of strings from text string divided to chunks with exact size
     */
    @Suppress("SameParameterValue")
    @JvmStatic
    private fun splitEqually(text: String, size: Int): List<String> {
        return ArrayList<String>((text.length + size - 1) / size).apply {
            var start = 0
            while (start < text.length) {
                add(text.substring(start, kotlin.math.min(text.length, start + size)))
                start += size
            }
        }
    }
}
