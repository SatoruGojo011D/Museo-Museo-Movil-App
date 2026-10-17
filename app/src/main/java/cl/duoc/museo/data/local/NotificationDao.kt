package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY id DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationItem>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadNotificationCount(userId: Long): Int

    @Query(
        "SELECT EXISTS(SELECT 1 FROM notifications WHERE userId = :userId " +
            "AND category = 'Escaner QR' AND message = :message)",
    )
    suspend fun hasScannedCode(userId: Long, message: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)
}