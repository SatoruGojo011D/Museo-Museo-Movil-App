package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val museumId: String,
    val museumName: String,
    val museumCountry: String,
    val imageUrl: String,
    val classification: String
)