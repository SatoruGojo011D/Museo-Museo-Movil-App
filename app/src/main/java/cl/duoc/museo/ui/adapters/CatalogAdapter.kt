package cl.duoc.museo.ui.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duoc.museo.R
import cl.duoc.museo.data.model.CatalogItem
import cl.duoc.museo.databinding.ItemCatalogProductBinding
import cl.duoc.museo.utils.MoneyFormatter
import cl.duoc.museo.utils.ViewAnimations
import com.bumptech.glide.Glide

class CatalogAdapter(
    private val onItemClick: (CatalogItem) -> Unit,
    private val onAddToCartClick: (CatalogItem) -> Unit,
    private val isAdministrator: () -> Boolean = { false },
) : ListAdapter<CatalogItem, CatalogAdapter.CatalogViewHolder>(CatalogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = ItemCatalogProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return CatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.animateItem()
    }

    inner class CatalogViewHolder(
        private val binding: ItemCatalogProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogItem) {
            ViewAnimations.installTapFeedback(binding.root)
            binding.tvProductTitle.text = item.title
            binding.tvProductCategory.text = item.category
            binding.tvProductPrice.text = MoneyFormatter.formatUsd(item.price)

            val imageRes = when (item.category) {
                "Cursos" -> R.drawable.ic_catalog
                "Libros" -> R.drawable.ic_book
                "Materiales" -> R.drawable.ic_museum
                else -> R.drawable.ic_catalog
            }
            binding.imgProductThumb.contentDescription = item.title
            Glide.with(binding.imgProductThumb)
                .load(item.imageUrl.takeIf { it.isNotBlank() })
                .placeholder(imageRes)
                .error(imageRes)
                .centerCrop()
                .into(binding.imgProductThumb)
            binding.btnAddToCart.visibility = if (isAdministrator()) View.GONE else View.VISIBLE

            binding.cardProduct.setOnClickListener { onItemClick(item) }
            binding.btnAddToCart.setOnClickListener { onAddToCartClick(item) }
        }

        fun animateItem() {
            itemView.alpha = 0f
            itemView.scaleX = 0.92f
            itemView.scaleY = 0.92f

            val alphaAnim = ObjectAnimator.ofFloat(itemView, View.ALPHA, 0f, 1f)
            val scaleXAnim = ObjectAnimator.ofFloat(itemView, View.SCALE_X, 0.92f, 1f)
            val scaleYAnim = ObjectAnimator.ofFloat(itemView, View.SCALE_Y, 0.92f, 1f)

            AnimatorSet().apply {
                playTogether(alphaAnim, scaleXAnim, scaleYAnim)
                duration = 350
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    class CatalogDiffCallback : DiffUtil.ItemCallback<CatalogItem>() {
        override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean {
            return oldItem == newItem
        }
    }
}