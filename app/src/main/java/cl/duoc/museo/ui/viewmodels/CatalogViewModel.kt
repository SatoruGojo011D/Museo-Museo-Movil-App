package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.CatalogItem
import cl.duoc.museo.data.model.DefaultCatalogItems
import cl.duoc.museo.data.model.Product
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CatalogEvent {
    data class Message(val text: String) : CatalogEvent()
}

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _allItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    private val _filteredItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogItems: StateFlow<List<CatalogItem>> = _filteredItems.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow("Recomendados")

    private val _userRole = MutableStateFlow(Role.CLIENT)
    val userRole: StateFlow<Role> = _userRole.asStateFlow()

    private val _isFabVisible = MutableStateFlow(false)
    val isFabVisible: StateFlow<Boolean> = _isFabVisible.asStateFlow()

    private val _events = MutableSharedFlow<CatalogEvent>()
    val events: SharedFlow<CatalogEvent> = _events.asSharedFlow()

    init {
        loadCatalogData()
    }

    fun loadCatalogData() {
        viewModelScope.launch {
            val role = sessionManager.getUserRole()
            _userRole.value = role
            _isFabVisible.value = (role == Role.ADMINISTRATOR)

            val cachedItems = repository.getAllCatalogItems()
            val catalogList = if (cachedItems.isNotEmpty()) {
                cachedItems.filter(CatalogItem::isActive)
            } else {
                val remoteProducts = repository.getProducts()
                val loadedItems = if (remoteProducts.isNotEmpty()) {
                    remoteProducts.map { product ->
                        CatalogItem(
                            id = product.id,
                            title = product.name,
                            category = product.category.ifBlank { "Libros" },
                            price = product.price,
                            imageUrl = product.imageUrl,
                            availability = if (product.stock > 0) "En stock" else "Agotado",
                            description = product.description,
                            type = if (product.category == "Cursos") "Curso" else "Producto",
                        )
                    }
                } else {
                    DefaultCatalogItems.items
                }
                repository.cacheCatalogItems(loadedItems)
                loadedItems
            }

            _allItems.value = catalogList
            applyFilterAndSearch()
        }
    }

    fun saveCatalogItem(
        existingItem: CatalogItem?,
        titleInput: String,
        category: String,
        priceInput: String,
        descriptionInput: String,
    ) {
        val title = titleInput.trim()
        val description = descriptionInput.trim()
        val price = priceInput.trim().toDoubleOrNull()
        if (title.isBlank()) {
            emitMessage("El nombre del producto o curso es obligatorio.")
            return
        }
        if (category !in setOf("Cursos", "Libros", "Materiales")) {
            emitMessage("Selecciona una categoría válida.")
            return
        }
        if (price == null || price <= 0.0) {
            emitMessage("Ingresa un precio mayor que cero.")
            return
        }

        viewModelScope.launch {
            try {
                val item = CatalogItem(
                    id = existingItem?.id ?: UUID.randomUUID().toString(),
                    title = title,
                    category = category,
                    price = price,
                    imageUrl = existingItem?.imageUrl.orEmpty(),
                    availability = existingItem?.availability ?: "En stock",
                    type = if (category == "Cursos") "Curso" else "Producto",
                    description = description,
                )
                repository.saveCatalogItem(item)
                _allItems.value = (_allItems.value.filterNot { it.id == item.id } + item)
                applyFilterAndSearch()
                _events.emit(CatalogEvent.Message("Catálogo actualizado correctamente."))
            } catch (exception: Exception) {
                _events.emit(
                    CatalogEvent.Message(
                        "No se pudo guardar el elemento: ${exception.localizedMessage ?: "inténtalo nuevamente."}",
                    ),
                )
            }
        }
    }

    fun deactivateCatalogItem(item: CatalogItem) {
        viewModelScope.launch {
            try {
                repository.deactivateCatalogItem(item.id)
                _allItems.value = _allItems.value.filterNot { it.id == item.id }
                applyFilterAndSearch()
                _events.emit(CatalogEvent.Message("Elemento desactivado del catálogo."))
            } catch (exception: Exception) {
                _events.emit(
                    CatalogEvent.Message(
                        "No se pudo desactivar el elemento: ${exception.localizedMessage ?: "inténtalo nuevamente."}",
                    ),
                )
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        applyFilterAndSearch()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilterAndSearch()
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value

        var filtered = _allItems.value

        if (category != "Todos") {
            filtered = filtered.filter { item ->
                item.category.equals(category, ignoreCase = true)
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.title.lowercase().contains(query) ||
                    item.description.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)
            }
        }

        _filteredItems.value = when (_sortOption.value) {
            "Precio: menor a mayor" -> filtered.sortedBy { it.price }
            "Precio: mayor a menor" -> filtered.sortedByDescending { it.price }
            "Nombre: A-Z" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered
        }
    }

    fun addToCart(item: CatalogItem, onAdded: () -> Unit) {
        val userId = sessionManager.getUserId()
        viewModelScope.launch {
            try {
                if (item.availability.equals("Agotado", ignoreCase = true)) {
                    _events.emit(CatalogEvent.Message("Este producto está agotado y no se puede agregar."))
                    return@launch
                }
                val product = Product(
                    id = item.id,
                    name = item.title,
                    category = item.category,
                    price = item.price,
                    imageUrl = item.imageUrl,
                    description = item.description,
                    stock = 10,
                )
                repository.addToCart(userId, product, quantity = 1)
                onAdded()
            } catch (exception: Exception) {
                _events.emit(
                    CatalogEvent.Message(
                        "No se pudo agregar al carrito: ${exception.localizedMessage ?: "inténtalo nuevamente."}",
                    ),
                )
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _events.emit(CatalogEvent.Message(message))
        }
    }
}