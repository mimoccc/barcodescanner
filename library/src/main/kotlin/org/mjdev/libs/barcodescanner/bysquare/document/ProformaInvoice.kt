package org.mjdev.libs.barcodescanner.bysquare.document

import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceBase
import org.mjdev.libs.barcodescanner.bysquare.decoder.InvoiceBaseDecoder

class ProformaInvoice(data: List<String>) : InvoiceBase() {
    init {
        InvoiceBaseDecoder(data).decode(this)
    }
}
