package dev.mkaminski.idealista.feature.detail

import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import dev.mkaminski.idealista.designsystem.ExternalLinks
import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.designsystem.IdealistaTheme
import dev.mkaminski.idealista.feature.detail.databinding.FragmentAdDetailBinding
import dev.mkaminski.idealista.model.AdDetail
import dev.mkaminski.idealista.model.AdLinks

/**
 * State-to-view mapping for the detail screen, lifted out of the Fragment.
 *
 * Free of the Fragment so it can be driven directly — by a test, or by the screenshot renderer,
 * which then captures the *real* screen rather than a re-implementation of it that can drift.
 */
internal fun FragmentAdDetailBinding.render(state: AdDetailUiState, gallery: GalleryAdapter) {
    loading.visibility = if (state is AdDetailUiState.Loading) View.VISIBLE else View.GONE
    errorState.visibility = if (state is AdDetailUiState.Error) View.VISIBLE else View.GONE
    content.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE
    favoriteFab.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE
    appBar.visibility = if (state is AdDetailUiState.Content) View.VISIBLE else View.GONE

    if (state !is AdDetailUiState.Content) return
    bindContent(state.detail, gallery, state.translatedComment)
}

internal fun FragmentAdDetailBinding.bindContent(
    detail: AdDetail,
    gallery: GalleryAdapter,
    translatedComment: String? = null,
) {
    val ad = detail.ad
    val context = root.context

    gallery.submitList(detail.gallery)
    galleryIndicator.visibility = if (detail.gallery.size > 1) View.VISIBLE else View.GONE

    // External destinations. The photo and map links use data the API really provides; the listing
    // URL is built from idealista's public URL shape and will not resolve for the mock property
    // codes — see AdLinks.
    toolbar.menu.findItem(R.id.action_open_listing)?.setOnMenuItemClickListener {
        ExternalLinks.openInBrowser(context, AdLinks.listingUrl(ad.propertyCode))
        true
    }
    openMap.setOnClickListener {
        ExternalLinks.openExternally(context, AdLinks.mapUri(ad.latitude, ad.longitude, ad.address))
    }

    price.text = Formatters.price(ad.price, ad.currencySuffix)
    address.text = ad.address
    location.text = listOfNotNull(ad.district, ad.municipality).joinToString(", ")
    // The API only ever writes Spanish. When a translation is available it replaces the original,
    // and a note says so — a translated listing that pretends to be the original is dishonest, and
    // a Spanish speaker needs to know they can switch back.
    val original = detail.comment.ifBlank { ad.description }
    description.text = translatedComment ?: original
    translatedNote.isVisible = translatedComment != null

    // Section summaries: a collapsed accordion still says how much is inside it.
    locationDetail.text = listOfNotNull(
        ad.district,
        ad.neighborhood,
        ad.municipality,
        ad.province,
    ).distinct().joinToString(" · ")
    locationSection.summary = ad.municipality

    val featureLabels = buildList {
        if (ad.features.hasAirConditioning) add(context.getString(R.string.detail_feature_air_conditioning))
        if (ad.features.hasBoxRoom) add(context.getString(R.string.detail_feature_box_room))
        if (ad.features.hasSwimmingPool) add(context.getString(R.string.detail_feature_pool))
        if (ad.features.hasTerrace) add(context.getString(R.string.detail_feature_terrace))
        if (ad.features.hasGarden) add(context.getString(R.string.detail_feature_garden))
        if (ad.exterior) add(context.getString(R.string.detail_feature_exterior))
        ad.parking?.let { parking ->
            if (parking.hasParkingSpace) {
                add(
                    context.getString(
                        if (parking.includedInPrice) {
                            R.string.detail_feature_parking_included
                        } else {
                            R.string.detail_feature_parking
                        },
                    ),
                )
            }
        }
    }
    features.removeAllViews()
    featureLabels.forEach { label ->
        features.addView(
            Chip(context).apply {
                text = label
                isClickable = false
                isCheckable = false
            },
        )
    }
    featuresSection.summary = if (featureLabels.isEmpty()) null else featureLabels.size.toString()
    featuresSection.isVisible = featureLabels.isNotEmpty()

    val favoritedAt = ad.favoritedAt
    if (favoritedAt != null) {
        favoritedOn.text = context.getString(
            R.string.favorite_saved_on,
            Formatters.favoritedDate(favoritedAt),
        )
        favoritedOn.visibility = View.VISIBLE
        favoriteFab.setText(R.string.detail_favorite_remove)
        favoriteFab.setIconResource(dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite_filled)
    } else {
        favoritedOn.visibility = View.GONE
        favoriteFab.setText(R.string.detail_favorite_add)
        favoriteFab.setIconResource(dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite)
    }

    // The characteristics panel is Compose inside this XML screen (ADR-0006). Labels are built here
    // so formatting and string lookup stay out of the composable.
    val labels = characteristicLabels(
        characteristics = detail.characteristics,
        roomsLabel = { context.getString(R.string.detail_rooms, it) },
        bathsLabel = { context.getString(R.string.detail_baths, it) },
        areaLabel = { Formatters.area(it.toDouble()) },
        floorLabel = { context.getString(R.string.detail_floor, it) },
        liftLabel = { context.getString(if (it) R.string.detail_lift else R.string.detail_no_lift) },
        communityCostsLabel = {
            context.getString(
                R.string.detail_community_costs,
                Formatters.price(it, ad.currencySuffix),
            )
        },
    )
    characteristics.setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    )
    characteristics.setContent {
        IdealistaTheme {
            CharacteristicsPanel(labels = labels, certificate = detail.energyCertificate)
        }
    }
    characteristicsSection.summary = labels.size.toString()
}
