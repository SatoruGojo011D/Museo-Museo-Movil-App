package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_museums")
data class Museum(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val project: String,
    val description: String,
    val classification: String,
    val imageUrl: String,
    val projectUrl: String,
    val lat: Double,
    val lng: Double,
    var isFavorite: Boolean = false
)