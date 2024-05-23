package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable

class CustomerParty : Party(), IVerifiable {
    var partyIdentification: String? = null
}
