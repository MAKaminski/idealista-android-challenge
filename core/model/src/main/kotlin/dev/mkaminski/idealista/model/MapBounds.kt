package dev.mkaminski.idealista.model

/**
 * The rectangle that contains every ad with coordinates, plus a little breathing room.
 *
 * Pure geometry in `:core:model` so the "where should the map open?" decision is unit-tested on the
 * JVM rather than asserted against a map widget. Ads without coordinates are ignored rather than
 * treated as (0, 0) — the Gulf of Guinea is a long way from Madrid.
 */
data class MapBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    val centerLatitude: Double get() = (south + north) / 2
    val centerLongitude: Double get() = (west + east) / 2

    companion object {
        /** Madrid, for the case where nothing has been cached yet — the data is all Madrid. */
        val MADRID = MapBounds(south = 40.38, west = -3.75, north = 40.47, east = -3.65)

        private const val PADDING_DEGREES = 0.004

        fun around(ads: List<Ad>): MapBounds {
            val located = ads.mapNotNull { ad ->
                val lat = ad.latitude ?: return@mapNotNull null
                val lon = ad.longitude ?: return@mapNotNull null
                lat to lon
            }
            if (located.isEmpty()) return MADRID

            return MapBounds(
                south = located.minOf { it.first } - PADDING_DEGREES,
                west = located.minOf { it.second } - PADDING_DEGREES,
                north = located.maxOf { it.first } + PADDING_DEGREES,
                east = located.maxOf { it.second } + PADDING_DEGREES,
            )
        }
    }
}

/** The ads a map can actually place. */
fun List<Ad>.withCoordinates(): List<Ad> = filter { it.latitude != null && it.longitude != null }
