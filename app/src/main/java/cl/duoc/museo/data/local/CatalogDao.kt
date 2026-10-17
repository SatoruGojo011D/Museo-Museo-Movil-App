package cl.duoc.museo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.museo.data.model.CatalogItem

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items WHERE isActive = 1 ORDER BY title")
    suspend fun getActiveItems(): List<CatalogItem>

    @Query("SELECT * FROM catalog_items ORDER BY title")
    suspend fun getAllItems(): List<CatalogItem>

    @Query("SELECT * FROM catalog_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: String): CatalogItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CatalogItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CatalogItem)

    @Query("UPDATE catalog_items SET isActive = 0 WHERE id = :itemId")
    suspend fun deactivateItem(itemId: String)
}
