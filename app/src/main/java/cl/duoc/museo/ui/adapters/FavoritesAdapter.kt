package cl.duoc.museo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duoc.museo.R
import cl.duoc.museo.data.model.Favorite
import cl.duoc.museo.databinding.ItemFavoriteMuseumBinding
import cl.duoc.museo.utils.ViewAnimations

class FavoritesAdapter(
    private val onItemClick: (Favorite) -> Unit,
    private val onRemoveClick: (Favorite) -> Unit,
) : ListAdapter<Favorite, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteMuseumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteMuseumBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Favorite) {
            ViewAnimations.installTapFeedback(binding.root)
            binding.tvMuseumName.text = item.museumName
            binding.tvMuseumLocation.text = item.museumCountry

            val imageRes = when {
                item.classification.contains("Arte", ignoreCase = true) -> R.drawable.bg_museum_interior
                item.classification.contains("Recurso", ignoreCase = true) -> R.drawable.ic_catalog
                else -> R.drawable.ic_museum
            }
            binding.imgThumb.setImageResource(imageRes)

            binding.cardFavorite.setOnClickListener { onItemClick(item) }
            binding.btnRemoveFavorite.setOnClickListener { onRemoveClick(item) }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<Favorite>() {
        override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
            return oldItem.id == newItem.id || oldItem.museumId == newItem.museumId
        }

        override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite): Boolean {
            return oldItem == newItem
        }
    }
}