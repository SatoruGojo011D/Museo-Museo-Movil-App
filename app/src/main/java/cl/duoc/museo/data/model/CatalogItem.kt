package cl.duoc.museo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_items")
data class CatalogItem(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String, // Cursos, Libros, Materiales
    val price: Double,
    val imageUrl: String,
    val availability: String = "En stock",
    val type: String = "Producto",
    val description: String = "",
    val rating: Double = 4.8,
    val isActive: Boolean = true,
)