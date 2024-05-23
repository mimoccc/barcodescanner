package org.mjdev.libs.barcodescanner.bysquare.base

import org.mjdev.libs.barcodescanner.bysquare.lzma.Decoder
import org.mjdev.libs.barcodescanner.bysquare.document.AdvanceInvoice
import org.mjdev.libs.barcodescanner.bysquare.document.CreditNote
import org.mjdev.libs.barcodescanner.bysquare.document.DebitNote
import org.mjdev.libs.barcodescanner.bysquare.document.Invoice
import org.mjdev.libs.barcodescanner.bysquare.document.InvoiceItems
import org.mjdev.libs.barcodescanner.bysquare.document.Pay
import org.mjdev.libs.barcodescanner.bysquare.document.ProformaInvoice
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * BySquare variable header field implementation
 * This header is first 4 bytes in base32hex encoded string,
 * 2 bytes in decoded.
 * Header signature indicates contents of data
 */
@Suppress("SpellCheckingInspection")
sealed class Header(
    var bysquareType: Int,
    var documentType: Int,
    var version: Int
) {
    /**
     * Header types
     */
    class AdvanceInvoice : Header(TYPE_INVOICE, DOCUMENT_ADVANCE_INVOICE, 0)
    class CreditNote : Header(TYPE_INVOICE, DOCUMENT_CREDIT_NOTE, 0)
    class DebitNote : Header(TYPE_INVOICE, DOCUMENT_DEBIT_NOTE, 0)
    class Invoice : Header(TYPE_INVOICE, DOCUMENT_INVOICE, 0)
    class InvoiceItems : Header(TYPE_INVOICE_ITEMS, DOCUMENT_INVOICE_ITEMS, 0)
    class Pay : Header(TYPE_PAY, DOCUMENT_PAY, 0)
    class ProformaInvoice : Header(TYPE_INVOICE, DOCUMENT_PROFORMA_INVOICE, 0)

    companion object {
        private const val HEADER_SIZE = 2

        const val TYPE_PAY = 0
        const val TYPE_INVOICE = 1
        const val TYPE_INVOICE_ITEMS = 2

        const val DOCUMENT_PAY = 0
        const val DOCUMENT_PROFORMA_INVOICE = 1
        const val DOCUMENT_CREDIT_NOTE = 2
        const val DOCUMENT_DEBIT_NOTE = 3
        const val DOCUMENT_ADVANCE_INVOICE = 4
        const val DOCUMENT_INVOICE = 0
        const val DOCUMENT_INVOICE_ITEMS = 0

        /**
         * All headers for lookup
         */
        private val headers = arrayOf(
            AdvanceInvoice(),
            CreditNote(),
            DebitNote(),
            Invoice(),
            InvoiceItems(),
            Pay(),
            ProformaInvoice()
        )

        /**
         * Search header type by its type, doctype and version
         * @param type document main type
         * @param version version of document
         * @param docType document subtype
         * @return header
         */
        @Throws(UnknownDocumentException::class)
        private fun find(type: Int, version: Int, docType: Int): Header {
            return headers.filter { h ->
                h.bysquareType == type && h.version == version && h.documentType == docType
            }.let { foundHeaders ->
                if (foundHeaders.isNotEmpty()) foundHeaders.first()
                else throw UnknownDocumentException("Unknown header found.")
            }
        }

        /**
         * Search header type by its type, doctype and version from data
         * @param bytes bytes of data
         * @return header
         */
        @Throws(UnknownDocumentException::class)
        private fun find(bytes: ByteArray): Header {
            return if (bytes.size >= HEADER_SIZE) {
                val type = (0xf0 and bytes[0].toInt()) shr 4
                val version = (0x0f and bytes[0].toInt())
                val docType = (0xf0 and bytes[1].toInt()) shr 4
                find(type, version, docType)
            } else {
                throw UnknownDocumentException("Input data is too short for bysquare header.")
            }
        }

        /**
         * Decode and return header from data
         * @param data bytes with data
         * @return header from data
         */
        fun decodeHeader(data: ByteArray): Header {
            if (data.size < HEADER_SIZE) {
                throw HeaderException("Header not found.")
            } else {
                return find(data)
            }
        }
    }

    /**
     * Parse data by header type and return them as parsing result
     */
    fun parse(data: ByteArray): BysquareDocument {
        // check checksum of decoded data and if equals, parse data
        CRC32Checksum().remove(decompress(data)).let{
            String(it).split('\t')
        }.let { payData ->
            return when (this) {
                is Pay -> Pay(payData)
                is AdvanceInvoice -> AdvanceInvoice(payData)
                is CreditNote -> CreditNote(payData)
                is DebitNote -> DebitNote(payData)
                is Invoice -> Invoice(payData)
                is ProformaInvoice -> ProformaInvoice(payData)
                is InvoiceItems -> InvoiceItems(payData)
            }
        }
    }

    /**
     * Decompress data from lzma 1
     * @param data lzma encoded bytes
     */
    private fun decompress(data: ByteArray): ByteArray {
        return Decoder().apply {
            setLcLpPb(3, 0, 2)
            setDictionarySize(1024 * 128)
        }.let { lzmaDecoder ->
            var ret: ByteArray? = null
            if (data.size >= 2) {
                val bais = ByteArrayInputStream(data)
                val ssize = (255 and bais.read()) + (255 and bais.read() shl 8)
                val baos = ByteArrayOutputStream(ssize)
                try {
                    lzmaDecoder.code(bais, baos, ssize.toLong())
                } catch (e: java.lang.Exception) {
                    // no op
                }
                if (baos.size() != 0) {
                    ret = baos.toByteArray()
                }
                baos.close()
                bais.close()
            }
            ret ?: ByteArray(0)
        }
    }

    override fun toString(): String {
        return this::class.simpleName.toString()
    }
}
