package cl.duoc.museo.data.model

data class Product(
    val id: String,
    val name: String,
    val category: String, // Cursos, Libros, Materiales
    val price: Double,
    val imageUrl: String,
    val description: String,
    val stock: Int
)