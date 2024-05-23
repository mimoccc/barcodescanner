package org.mjdev.libs.barcodescanner.bysquare.base

import java.util.zip.CRC32

/**
 * Class compute and remove from data CRC32 CheckSum
 * This class is to check that data are correct after decompress
 */
class CRC32Checksum {
    companion object {
        /**
         * Checksum is CRC32 32 bit integer, 4 bytes
         */
        private const val CHECKSUM_SIZE = 4
    }

    private val crc32 = CRC32()

    /**
     * Calculate custom checksum from data
     * @param data bytes to crete checksum from
     * @param offset start point of data
     * @param len size of data
     */
    @Suppress("SameParameterValue")
    private fun calculate(data: ByteArray, offset: Int, len: Int): Long {
        crc32.reset()
        crc32.update(data, offset, len)
        return crc32.value
    }

    /**
     * Remove checksum from data (first 4 bytes), if checksums are ok
     * @param bytes data to check
     */
    fun remove(bytes: ByteArray): ByteArray {
        return if (bytes.size >= CHECKSUM_SIZE) {
            val check1 = 255 and bytes[0].toInt() or
                    (255 and bytes[1].toInt() shl 8) or
                    (255 and bytes[2].toInt() shl 16) or
                    (255 and bytes[3].toInt() shl 24)
            val check2 = calculate(bytes, CHECKSUM_SIZE, bytes.size - CHECKSUM_SIZE).toInt()
            if (check1 == check2) {
                Utils.copyOfRange(bytes, CHECKSUM_SIZE, bytes.size - CHECKSUM_SIZE)
            } else {
                ByteArray(0)
            }
        } else {
            ByteArray(0)
        }
    }
}
