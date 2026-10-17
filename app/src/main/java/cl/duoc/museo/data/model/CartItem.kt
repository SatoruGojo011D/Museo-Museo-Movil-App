package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String
)