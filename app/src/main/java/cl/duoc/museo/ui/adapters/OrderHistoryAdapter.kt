package cl.duoc.museo.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duoc.museo.data.model.Order
import cl.duoc.museo.databinding.ItemOrderHistoryBinding
import cl.duoc.museo.utils.ViewAnimations

class OrderHistoryAdapter(
    private val onOrderClick: (Order) -> Unit,
) : ListAdapter<Order, OrderHistoryAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderHistoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            ViewAnimations.installTapFeedback(binding.root)
            val formattedId = if (order.id > 0) {
                "#2024-${order.id.toString().padStart(5, '0')}"
            } else {
                "#2024-00123"
            }

            binding.tvOrderId.text = formattedId
            binding.tvOrderDate.text = order.date
            val countStr = if (order.productCount == 1) "1 producto" else "${order.productCount} productos"
            binding.tvItemsCount.text = countStr
            binding.tvTotalPrice.text = "US$ ${order.totalAmount.toInt()}"
            binding.tvStatusBadge.text = order.status

            // Dynamic Status Badge Color Styling
            val (bgColor, textColor) = when (order.status.lowercase()) {
                "entregado", "entregadas" -> Pair("#E8F5E9", "#2E7D32")
                "enviado", "enviadas", "en camino" -> Pair("#E3F2FD", "#1565C0")
                "confirmado" -> Pair("#FFF8E1", "#F57F17")
                "pendiente", "pendientes" -> Pair("#FFF3E0", "#E65100")
                else -> Pair("#E8F5E9", "#2E7D32")
            }

            binding.tvStatusBadge.background.setTint(Color.parseColor(bgColor))
            binding.tvStatusBadge.setTextColor(Color.parseColor(textColor))

            binding.cardOrder.setOnClickListener { onOrderClick(order) }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}