package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.CartItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItemsForUser(userId: Long): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getCartItemByProduct(userId: Long, productId: String): CartItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem): Long

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateQuantity(itemId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE id = :itemId")
    suspend fun deleteCartItem(itemId: Long)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCartForUser(userId: Long)
}