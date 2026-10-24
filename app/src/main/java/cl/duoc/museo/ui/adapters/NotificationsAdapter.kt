package cl.duoc.museo.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cl.duoc.museo.R
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.databinding.ItemNotificationBinding
import cl.duoc.museo.utils.ViewAnimations

class NotificationsAdapter(
    private val onNotificationClick: (NotificationItem) -> Unit,
) : ListAdapter<NotificationItem, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            ViewAnimations.installTapFeedback(binding.root)
            binding.tvNotifTitle.text = item.title
            binding.tvNotifMessage.text = item.message
            binding.tvTimestamp.text = item.timestamp
            binding.vUnreadDot.visibility = if (!item.isRead) View.VISIBLE else View.GONE

            // Category Dynamic Circle Icon and Color Tint
            val (iconRes, bgHex) = when (item.category.lowercase()) {
                "cursos" -> Pair(R.drawable.ic_catalog, "#4A6572")
                "museos" -> Pair(R.drawable.ic_museum, "#D4B886")
                "pedidos" -> Pair(R.drawable.ic_cart, "#1E88E5")
                "eventos", "alertas" -> Pair(R.drawable.ic_notifications, "#E53935")
                "publicaciones" -> Pair(R.drawable.ic_catalog, "#43A047")
                else -> Pair(R.drawable.ic_museum, "#0F3A5A")
            }

            binding.imgTypeIcon.setImageResource(iconRes)
            binding.imgTypeIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgHex))

            binding.layoutNotificationRow.setOnClickListener { onNotificationClick(item) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem,
        ): Boolean {
            return oldItem == newItem
        }
    }
}