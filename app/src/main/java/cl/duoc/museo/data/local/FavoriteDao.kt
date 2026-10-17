package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesForUser(userId: Long): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite): Long

    @Query("DELETE FROM favorites WHERE userId = :userId AND museumId = :museumId")
    suspend fun deleteFavorite(userId: Long, museumId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND museumId = :museumId LIMIT 1)")
    suspend fun isFavorite(userId: Long, museumId: String): Boolean
}