package dev.mkaminski.idealista.model

/**
 * Search criteria over the fields `list.json` actually provides.
 *
 * Deliberately limited to available data: floor, lift and community costs are detail-only, so there
 * is no filter for them — a filter that silently matches nothing is worse than no filter.
 */
data class AdFilters(
    val operation: Operation? = null,
    val minRooms: Int? = null,
    val minBathrooms: Int? = null,
    val maxPrice: Double? = null,
    val minSizeSquareMeters: Double? = null,
    val exteriorOnly: Boolean = false,
    val withParking: Boolean = false,
    val amenities: Set<Amenity> = emptySet(),
    val favoritesOnly: Boolean = false,
    val sort: AdSort = AdSort.DEFAULT,
) {
    val isActive: Boolean
        get() = operation != null ||
            minRooms != null ||
            minBathrooms != null ||
            maxPrice != null ||
            minSizeSquareMeters != null ||
            exteriorOnly ||
            withParking ||
            amenities.isNotEmpty() ||
            favoritesOnly

    /** How many criteria are on — shown on the filter button so the state is never invisible. */
    val activeCount: Int
        get() = listOf(
            operation != null,
            minRooms != null,
            minBathrooms != null,
            maxPrice != null,
            minSizeSquareMeters != null,
            exteriorOnly,
            withParking,
            favoritesOnly,
        ).count { it } + amenities.size
}

enum class Amenity {
    AIR_CONDITIONING,
    BOX_ROOM,
    SWIMMING_POOL,
    TERRACE,
    GARDEN,
}

enum class AdSort {
    /** The order the API returned, so "no sort" is a real choice rather than a hidden default. */
    DEFAULT,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    SIZE_LARGEST,
    ROOMS_MOST,
    RECENTLY_FAVORITED,
}

private fun Ad.has(amenity: Amenity): Boolean = when (amenity) {
    Amenity.AIR_CONDITIONING -> features.hasAirConditioning
    Amenity.BOX_ROOM -> features.hasBoxRoom
    Amenity.SWIMMING_POOL -> features.hasSwimmingPool
    Amenity.TERRACE -> features.hasTerrace
    Amenity.GARDEN -> features.hasGarden
}

fun Ad.matches(filters: AdFilters): Boolean {
    filters.operation?.let { if (operation != it) return false }
    filters.minRooms?.let { if (rooms < it) return false }
    filters.minBathrooms?.let { if (bathrooms < it) return false }
    filters.maxPrice?.let { if (price > it) return false }
    filters.minSizeSquareMeters?.let { if (sizeSquareMeters < it) return false }
    if (filters.exteriorOnly && !exterior) return false
    if (filters.withParking && parking?.hasParkingSpace != true) return false
    if (filters.favoritesOnly && !isFavorite) return false
    // Amenities are AND-ed: selecting "pool" and "garden" means both, as a property search should.
    if (!filters.amenities.all { has(it) }) return false
    return true
}

fun List<Ad>.applyFilters(filters: AdFilters): List<Ad> {
    val matching = filter { it.matches(filters) }
    return when (filters.sort) {
        AdSort.DEFAULT -> matching
        AdSort.PRICE_LOW_TO_HIGH -> matching.sortedBy { it.price }
        AdSort.PRICE_HIGH_TO_LOW -> matching.sortedByDescending { it.price }
        AdSort.SIZE_LARGEST -> matching.sortedByDescending { it.sizeSquareMeters }
        AdSort.ROOMS_MOST -> matching.sortedByDescending { it.rooms }
        // Nulls last: unfavorited ads keep the API's order behind the favorited ones.
        AdSort.RECENTLY_FAVORITED -> matching.sortedByDescending { it.favoritedAt }
    }
}
