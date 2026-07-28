package dev.mkaminski.idealista.feature.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import dev.mkaminski.idealista.feature.detail.databinding.ItemGalleryImageBinding
import dev.mkaminski.idealista.model.AdImage

internal class GalleryAdapter : ListAdapter<AdImage, GalleryAdapter.ImageViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageViewHolder(
        ItemGalleryImageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) =
        holder.bind(getItem(position), position, itemCount)

    internal class ImageViewHolder(
        private val binding: ItemGalleryImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: AdImage, position: Int, total: Int) {
            val placeholderRes = dev.mkaminski.idealista.designsystem.R.drawable.image_placeholder
            binding.galleryImage.load(image.url) {
                placeholder(placeholderRes)
                error(placeholderRes)
            }
            // The detail payload localizes each photo's room name; use it rather than "image".
            binding.galleryImage.contentDescription = binding.root.context.getString(
                R.string.detail_gallery_image,
                position + 1,
                total,
                image.localizedName ?: image.tag.orEmpty(),
            )
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdImage>() {
            override fun areItemsTheSame(oldItem: AdImage, newItem: AdImage) =
                oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: AdImage, newItem: AdImage) = oldItem == newItem
        }
    }
}
