package org.mjdev.libs.barcodescanner.bysquare.document

import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceBase
import org.mjdev.libs.barcodescanner.bysquare.decoder.InvoiceBaseDecoder

class Invoice(data: List<String>) : InvoiceBase() {
    init {
        InvoiceBaseDecoder(data).decode(this)
    }
}
