package cl.duoc.museo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duoc.museo.R
import cl.duoc.museo.data.model.CartItem
import cl.duoc.museo.databinding.ItemCartProductBinding

class CartAdapter(
    private val onIncrementClick: (CartItem) -> Unit,
    private val onDecrementClick: (CartItem) -> Unit,
    private val onDeleteClick: (CartItem) -> Unit,
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class CartViewHolder(
        private val binding: ItemCartProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvCartItemTitle.text = item.productName
            binding.tvCartItemPrice.text = "US$ ${item.price.toInt()}"
            binding.tvCartQuantity.text = item.quantity.toString()

            val imageRes = if (item.productName.contains("Curso", ignoreCase = true)) {
                R.drawable.ic_catalog
            } else {
                R.drawable.bg_museum_interior
            }
            binding.imgCartThumb.setImageResource(imageRes)

            binding.btnCartPlus.setOnClickListener { onIncrementClick(item) }
            binding.btnCartMinus.setOnClickListener { onDecrementClick(item) }
            binding.btnDeleteCartItem.setOnClickListener { onDeleteClick(item) }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}