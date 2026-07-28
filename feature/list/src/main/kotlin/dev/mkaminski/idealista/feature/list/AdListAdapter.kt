package dev.mkaminski.idealista.feature.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import dev.mkaminski.idealista.designsystem.Formatters
import dev.mkaminski.idealista.feature.list.databinding.ItemAdBinding
import dev.mkaminski.idealista.model.Ad

internal class AdListAdapter(
    private val onAdClick: (Ad) -> Unit,
    private val onFavoriteClick: (Ad) -> Unit,
) : ListAdapter<Ad, AdListAdapter.AdViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder =
        AdViewHolder(
            ItemAdBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAdClick,
            onFavoriteClick,
        )

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) =
        holder.bind(getItem(position))

    internal class AdViewHolder(
        private val binding: ItemAdBinding,
        private val onAdClick: (Ad) -> Unit,
        private val onFavoriteClick: (Ad) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: Ad) = with(binding) {
            val context = root.context
            val model = ad.toCardUiModel()

            price.text = model.price
            address.text = model.address
            summary.text = context.getString(
                R.string.list_summary,
                model.roomCount,
                model.bathCount,
                model.area,
            )

            // Tagged with the ad it belongs to, so a recycled row showing a stale image is a
            // detectable state rather than a subtle visual bug.
            thumbnail.tag = model.propertyCode
            thumbnail.contentDescription =
                context.getString(R.string.ad_thumbnail_description, ad.address)
            thumbnail.load(model.imageUrl) {
                placeholder(dev.mkaminski.idealista.designsystem.R.drawable.image_placeholder)
                error(dev.mkaminski.idealista.designsystem.R.drawable.image_placeholder)
            }

            // The date is the requirement, not a nicety: an ad shows *when* it was favorited.
            val favoritedAt = model.favoritedAt
            if (favoritedAt != null) {
                favoritedOn.text = context.getString(
                    R.string.favorite_saved_on,
                    Formatters.favoritedDate(favoritedAt),
                )
                favoritedOn.visibility = android.view.View.VISIBLE
            } else {
                favoritedOn.visibility = android.view.View.GONE
            }

            favoriteToggle.setIconResource(
                if (model.isFavorite) {
                    dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite_filled
                } else {
                    dev.mkaminski.idealista.designsystem.R.drawable.ic_favorite
                },
            )
            favoriteToggle.contentDescription = context.getString(
                if (model.isFavorite) R.string.favorite_remove else R.string.favorite_add,
            )

            adCard.setOnClickListener { onAdClick(ad) }
            favoriteToggle.setOnClickListener { onFavoriteClick(ad) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Ad>() {
            override fun areItemsTheSame(oldItem: Ad, newItem: Ad) =
                oldItem.propertyCode == newItem.propertyCode

            override fun areContentsTheSame(oldItem: Ad, newItem: Ad) = oldItem == newItem
        }
    }
}
