package org.mjdev.libs.barcodescanner.bysquare.document

import org.mjdev.libs.barcodescanner.bysquare.data.PayBase
import org.mjdev.libs.barcodescanner.bysquare.decoder.PayBaseDecoder

class DebitNote(data: List<String>) : PayBase() {
    init {
        PayBaseDecoder(data).decode(this)
    }
}
