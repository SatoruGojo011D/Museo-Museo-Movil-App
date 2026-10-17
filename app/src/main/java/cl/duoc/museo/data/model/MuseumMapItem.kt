package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_museums_map")
data class MuseumMapItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val location: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val virtualTourUrl: String, // Differentiates geographical location (lat, lng) from the virtual tour URL
    val imageUrl: String,
    val rating: Double = 4.8,
    val reviewsCount: Int = 120,
    val tag1: String = "Historia",
    val tag2: String = "Virtual",
    val country: String = "",
)