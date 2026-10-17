package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.Museum
import kotlinx.coroutines.flow.Flow

@Dao
interface MuseumDao {
    @Query("SELECT * FROM cached_museums")
    fun getAllMuseums(): Flow<List<Museum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(museums: List<Museum>)

    @Query("SELECT * FROM cached_museums WHERE id = :museumId LIMIT 1")
    suspend fun getMuseumById(museumId: String): Museum?
}