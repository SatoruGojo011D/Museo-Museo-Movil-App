package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val category: String, // "Pedidos", "Cursos", "Museos"
    val timestamp: String,
    val isRead: Boolean = false
)