package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: String,
    val productCount: Int,
    val totalAmount: Double,
    val status: String, // "Entregado", "Pendiente", "En camino"
    val country: String,
    val shippingType: String
)