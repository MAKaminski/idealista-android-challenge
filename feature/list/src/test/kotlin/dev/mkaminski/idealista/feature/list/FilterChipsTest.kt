package dev.mkaminski.idealista.feature.list

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dev.mkaminski.idealista.model.AdFilters
import dev.mkaminski.idealista.model.AdSort
import dev.mkaminski.idealista.model.Amenity
import dev.mkaminski.idealista.model.Operation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import android.view.ContextThemeWrapper

/**
 * The chip row is rebuilt from [AdFilters] on every emission, so a chip can never show a state the
 * ViewModel does not hold. These tests pin that both ways: state renders into chips, and a tap
 * produces the transform the state expects.
 */
@RunWith(RobolectricTestRunner::class)
class FilterChipsTest {

    private val context = ContextThemeWrapper(
        Robolectric.buildActivity(Activity::class.java).get(),
        com.google.android.material.R.style.Theme_Material3_DayNight,
    )

    private fun group() = ChipGroup(context)

    private fun ChipGroup.chipLabelled(resId: Int): Chip {
        val label = context.getString(resId)
        return (0 until childCount)
            .map { getChildAt(it) as Chip }
            .single { it.text.toString() == label }
    }

    @Test
    fun `every spec renders a chip`() {
        val group = group()

        group.bindFilters(AdFilters(), onToggle = {}, onClear = {})

        assertEquals(filterChipSpecs.size, group.childCount)
    }

    @Test
    fun `a chip is checked when its filter is active`() {
        val group = group()

        group.bindFilters(AdFilters(operation = Operation.RENT), onToggle = {}, onClear = {})

        assertTrue(group.chipLabelled(R.string.filter_rent).isChecked)
        assertFalse(group.chipLabelled(R.string.filter_sale).isChecked)
    }

    @Test
    fun `tapping a chip reports the transform that turns its filter on`() {
        val group = group()
        var result: AdFilters? = null
        group.bindFilters(AdFilters(), onToggle = { result = it(AdFilters()) }, onClear = {})

        group.chipLabelled(R.string.filter_pool).performClick()

        assertEquals(setOf(Amenity.SWIMMING_POOL), result?.amenities)
    }

    /** Tapping an active chip must clear it, not re-apply it — the chips are toggles. */
    @Test
    fun `tapping an active chip reports the transform that turns it off`() {
        val active = AdFilters(operation = Operation.SALE)
        val group = group()
        var result: AdFilters? = null
        group.bindFilters(active, onToggle = { result = it(active) }, onClear = {})

        group.chipLabelled(R.string.filter_sale).performClick()

        assertEquals(null, result?.operation)
    }

    @Test
    fun `the sort chip toggles back to the default order`() {
        val sorted = AdFilters(sort = AdSort.PRICE_LOW_TO_HIGH)
        val group = group()
        var result: AdFilters? = null
        group.bindFilters(sorted, onToggle = { result = it(sorted) }, onClear = {})

        group.chipLabelled(R.string.filter_price_low).performClick()

        assertEquals(AdSort.DEFAULT, result?.sort)
    }

    /** No point offering "clear" when there is nothing to clear. */
    @Test
    fun `the clear chip appears only once a filter is active`() {
        val group = group()

        group.bindFilters(AdFilters(), onToggle = {}, onClear = {})
        assertEquals(filterChipSpecs.size, group.childCount)

        group.bindFilters(AdFilters(exteriorOnly = true), onToggle = {}, onClear = {})
        assertEquals(filterChipSpecs.size + 1, group.childCount)
    }

    @Test
    fun `tapping clear reports a clear rather than a toggle`() {
        val group = group()
        var cleared = false
        group.bindFilters(
            AdFilters(withParking = true),
            onToggle = { error("a toggle must not be reported for the clear chip") },
            onClear = { cleared = true },
        )

        group.chipLabelled(R.string.filter_clear).performClick()

        assertTrue(cleared)
    }

    /** Rebuilding from state is what stops the row from drifting; assert it really rebuilds. */
    @Test
    fun `rebinding replaces the row rather than appending to it`() {
        val group = group()

        group.bindFilters(AdFilters(), onToggle = {}, onClear = {})
        group.bindFilters(AdFilters(), onToggle = {}, onClear = {})

        assertEquals(filterChipSpecs.size, group.childCount)
    }
}
