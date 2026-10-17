package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY id DESC")
    fun getOrdersForUser(userId: Long): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, newStatus: String)
}