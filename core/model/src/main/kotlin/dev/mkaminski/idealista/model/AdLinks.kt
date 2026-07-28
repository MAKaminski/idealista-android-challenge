package dev.mkaminski.idealista.model

/**
 * The external destinations an ad can point at.
 *
 * The mock API gives no canonical listing URL, so [listingUrl] is built from idealista's public URL
 * shape. **With the mock `propertyCode` values (1–4) it will not resolve to a real listing** — the
 * wiring is real, the ids are not. Photo and map links, by contrast, use data the API really
 * provides and work as expected.
 */
object AdLinks {

    private const val LISTING_BASE = "https://www.idealista.com/inmueble"

    fun listingUrl(propertyCode: String): String = "$LISTING_BASE/$propertyCode/"

    /**
     * A `geo:` URI, so the device's own map app handles it. Falls back to a maps web search when the
     * ad has no coordinates, which keeps the action available rather than disabled.
     */
    fun mapUri(latitude: Double?, longitude: Double?, label: String?): String =
        if (latitude != null && longitude != null) {
            val query = if (label.isNullOrBlank()) {
                "$latitude,$longitude"
            } else {
                "$latitude,$longitude(${label.encodeForUri()})"
            }
            "geo:$latitude,$longitude?q=$query"
        } else {
            "https://www.google.com/maps/search/?api=1&query=" +
                (label ?: "").encodeForUri()
        }

    private fun String.encodeForUri(): String = replace(" ", "%20").replace(",", "%2C")
}
