package dev.mkaminski.idealista.feature.list

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dev.mkaminski.idealista.model.AdFilters
import dev.mkaminski.idealista.model.AdSort
import dev.mkaminski.idealista.model.Amenity
import dev.mkaminski.idealista.model.Operation

/**
 * The filter row.
 *
 * Only criteria the **list** payload can answer are offered — lift, floor and community costs are
 * detail-only, and a filter that quietly matches nothing is worse than no filter at all.
 * Each chip is a pure transform of [AdFilters], so the behaviour is covered by the model's tests.
 */
internal data class FilterChipSpec(
    @param:StringRes val label: Int,
    val isChecked: (AdFilters) -> Boolean,
    val toggle: (AdFilters) -> AdFilters,
)

internal val filterChipSpecs: List<FilterChipSpec> = listOf(
    FilterChipSpec(
        label = R.string.filter_sale,
        isChecked = { it.operation == Operation.SALE },
        toggle = { it.copy(operation = if (it.operation == Operation.SALE) null else Operation.SALE) },
    ),
    FilterChipSpec(
        label = R.string.filter_rent,
        isChecked = { it.operation == Operation.RENT },
        toggle = { it.copy(operation = if (it.operation == Operation.RENT) null else Operation.RENT) },
    ),
    FilterChipSpec(
        label = R.string.filter_favorites,
        isChecked = { it.favoritesOnly },
        toggle = { it.copy(favoritesOnly = !it.favoritesOnly) },
    ),
    FilterChipSpec(
        label = R.string.filter_rooms,
        isChecked = { it.minRooms == MIN_ROOMS },
        toggle = { it.copy(minRooms = if (it.minRooms == MIN_ROOMS) null else MIN_ROOMS) },
    ),
    FilterChipSpec(
        label = R.string.filter_baths,
        isChecked = { it.minBathrooms == MIN_BATHS },
        toggle = { it.copy(minBathrooms = if (it.minBathrooms == MIN_BATHS) null else MIN_BATHS) },
    ),
    FilterChipSpec(
        label = R.string.filter_exterior,
        isChecked = { it.exteriorOnly },
        toggle = { it.copy(exteriorOnly = !it.exteriorOnly) },
    ),
    FilterChipSpec(
        label = R.string.filter_parking,
        isChecked = { it.withParking },
        toggle = { it.copy(withParking = !it.withParking) },
    ),
    amenityChip(R.string.filter_air_conditioning, Amenity.AIR_CONDITIONING),
    amenityChip(R.string.filter_pool, Amenity.SWIMMING_POOL),
    amenityChip(R.string.filter_terrace, Amenity.TERRACE),
    amenityChip(R.string.filter_garden, Amenity.GARDEN),
    FilterChipSpec(
        label = R.string.filter_price_low,
        isChecked = { it.sort == AdSort.PRICE_LOW_TO_HIGH },
        toggle = {
            it.copy(
                sort = if (it.sort == AdSort.PRICE_LOW_TO_HIGH) AdSort.DEFAULT else AdSort.PRICE_LOW_TO_HIGH,
            )
        },
    ),
)

private fun amenityChip(@StringRes label: Int, amenity: Amenity) = FilterChipSpec(
    label = label,
    isChecked = { amenity in it.amenities },
    toggle = {
        it.copy(
            amenities = if (amenity in it.amenities) it.amenities - amenity else it.amenities + amenity,
        )
    },
)

/**
 * Rebuilds the row from state rather than mutating chips in place, so the chips can never drift out
 * of sync with the filters they represent.
 */
internal fun ChipGroup.bindFilters(
    filters: AdFilters,
    onToggle: ((AdFilters) -> AdFilters) -> Unit,
    onClear: () -> Unit,
) {
    val context: Context = context
    removeAllViews()

    filterChipSpecs.forEach { spec ->
        addView(
            Chip(context).apply {
                setText(spec.label)
                isCheckable = true
                isChecked = spec.isChecked(filters)
                setOnClickListener { onToggle(spec.toggle) }
            },
        )
    }

    if (filters.isActive) {
        addView(
            Chip(context).apply {
                setText(R.string.filter_clear)
                isCheckable = false
                isCloseIconVisible = false
                setOnClickListener { onClear() }
            },
        )
    }
}

private const val MIN_ROOMS = 3
private const val MIN_BATHS = 2
