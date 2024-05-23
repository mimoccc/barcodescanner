package org.mjdev.libs.barcodescanner.bysquare.data.invoice

import org.mjdev.libs.barcodescanner.bysquare.base.IVerifiable
import org.mjdev.libs.barcodescanner.bysquare.base.InvalidValueException
import org.mjdev.libs.barcodescanner.bysquare.base.Verify

class PostalAddress : IVerifiable {
    var buildingNumber: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var cityName: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var country: String? = null
        set(value) {
            field = Verify.trimAllToUpperCase(value)
        }
    var postalZone: String? = null
        set(value) {
            field = Verify.trimAll(value)
        }
    var state: String? = null
        set(value) {
            field = Verify.trim(value)
        }
    var streetName: String? = null
        set(value) {
            field = Verify.trim(value)
        }

    val isEmpty: Boolean
        get() = streetName == null && buildingNumber == null && cityName == null &&
                postalZone == null && state == null && country == null

    @Throws(InvalidValueException::class)
    override fun verify() {
        Verify.notNull("StreetName", streetName)
        Verify.notNull("CityName", cityName)
        Verify.notNull("PostalZone", postalZone)
        Verify.notNullAndMatch("Country", country, Verify.REGEX_COUNTRY_CODE)
    }
}
