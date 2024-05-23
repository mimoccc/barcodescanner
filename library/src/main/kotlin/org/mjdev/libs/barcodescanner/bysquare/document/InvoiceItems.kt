package org.mjdev.libs.barcodescanner.bysquare.document

import org.mjdev.libs.barcodescanner.bysquare.data.InvoiceItemsBase
import org.mjdev.libs.barcodescanner.bysquare.decoder.InvoiceItemsBaseDecoder

class InvoiceItems(data: List<String>) : InvoiceItemsBase() {
    init {
        InvoiceItemsBaseDecoder(data).decode(this)
    }
}
