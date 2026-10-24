package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.DefaultCatalogItems
import cl.duoc.museo.data.model.Product
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CartEvent {
    object Success : CartEvent()
    data class OutOfStock(val message: String) : CartEvent()
    data class Error(val message: String) : CartEvent()
}

data class ProductDetailUiState(
    val product: Product? = null,
    val errorMessage: String? = null,
    val quantity: Int = 1,
    val isFavorite: Boolean = false,
    val isStockAvailable: Boolean = true,
    val stockMessage: String = "En stock",
    val isCourse: Boolean = false,
    val buttonText: String = "Agregar al carrito",
    val isLoading: Boolean = false,
)

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _cartEvent = MutableSharedFlow<CartEvent>()
    val cartEvent: SharedFlow<CartEvent> = _cartEvent.asSharedFlow()

    private var currentProductId: String = "cat_01"

    fun initProduct(productId: String?, productName: String?) {
        val targetId = if (!productId.isNullOrBlank()) productId else "cat_01"
        currentProductId = targetId
        loadProductData(targetId, productName)
    }

    private fun loadProductData(productId: String, fallbackName: String?) {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState(isLoading = true)

            val userId = sessionManager.getUserId()

            // 1. Fetch products from Retrofit API / Room local cache via Repository
            val cachedItem = repository.getCatalogItem(productId)?.takeIf { it.isActive }
            val remoteProducts = if (cachedItem == null) repository.getProducts() else emptyList()
            val matchedProduct = cachedItem?.let { item ->
                Product(
                    id = item.id,
                    name = item.title,
                    category = item.category,
                    price = item.price,
                    imageUrl = item.imageUrl,
                    description = item.description,
                    stock = if (item.availability.equals("Agotado", ignoreCase = true)) 0 else 10,
                )
            } ?: remoteProducts.find { it.id == productId }
                ?: DefaultCatalogItems.items.find { it.id == productId }?.let { item ->
                    Product(
                        id = item.id,
                        name = item.title,
                        category = item.category,
                        price = item.price,
                        imageUrl = item.imageUrl,
                        description = item.description,
                        stock = if (item.availability.equals("Agotado", ignoreCase = true)) 0 else 10,
                    )
                }

            if (matchedProduct == null) {
                _uiState.value = ProductDetailUiState(
                    errorMessage = "No encontramos los detalles de ${fallbackName ?: "este producto"}. Vuelve al catálogo e inténtalo otra vez.",
                )
                return@launch
            }

            // Check if item is a Course vs Physical Product
            val isCourse = matchedProduct.category.equals("Cursos", ignoreCase = true) ||
                matchedProduct.name.contains("Curso", ignoreCase = true) ||
                matchedProduct.name.contains("Taller", ignoreCase = true)

            val isStockAvailable = matchedProduct.stock > 0
            val stockMessage = when {
                isCourse -> "Inscripción Abierta"
                isStockAvailable -> "En stock"
                else -> "Agotado"
            }

            val buttonLabel = if (isCourse) "Solicitar información" else "Agregar al carrito"

            // Check favorite state for active user profile in Room
            val isFav = repository.isFavorite(userId, productId)

            _uiState.value = ProductDetailUiState(
                product = matchedProduct,
                quantity = 1,
                isFavorite = isFav,
                isStockAvailable = isStockAvailable,
                stockMessage = stockMessage,
                isCourse = isCourse,
                buttonText = buttonLabel,
                isLoading = false,
            )
        }
    }

    fun incrementQuantity() {
        if (_uiState.value.isCourse) return
        val current = _uiState.value.quantity
        _uiState.value = _uiState.value.copy(quantity = current + 1)
    }

    fun decrementQuantity() {
        if (_uiState.value.isCourse) return
        val current = _uiState.value.quantity
        if (current > 1) {
            _uiState.value = _uiState.value.copy(quantity = current - 1)
        }
    }

    fun addToCart() {
        val state = _uiState.value
        val product = state.product ?: return
        val userId = sessionManager.getUserId()

        // Critical Requirement: Validate item stock/availability before allowing request/cart addition
        if (!state.isStockAvailable) {
            viewModelScope.launch {
                _cartEvent.emit(CartEvent.OutOfStock("Este producto se encuentra agotado actualmente."))
            }
            return
        }

        viewModelScope.launch {
            try {
                if (state.isCourse) {
                    // Log course information request in notification history
                    repository.addNotification(
                        cl.duoc.museo.data.model.NotificationItem(
                            userId = userId,
                            title = "Solicitud de Información: ${product.name}",
                            message = "Tu solicitud sobre el ${product.name} fue enviada a coordinación académica.",
                            category = "Cursos",
                            timestamp = "Hace un momento",
                        )
                    )
                } else {
                    // Add physical product item + selected quantity into Room database
                    repository.addToCart(userId, product, state.quantity)
                }
                _cartEvent.emit(CartEvent.Success)
            } catch (e: Exception) {
                _cartEvent.emit(CartEvent.Error("Error al agregar al carrito: ${e.localizedMessage}"))
            }
        }
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        val userId = sessionManager.getUserId()

        viewModelScope.launch {
            val museumAdapter = Museum(
                id = product.id,
                name = product.name,
                country = "Iberoamérica",
                project = product.category,
                description = product.description,
                classification = product.category,
                imageUrl = product.imageUrl,
                projectUrl = "",
                lat = 0.0,
                lng = 0.0,
            )
            repository.toggleFavorite(userId, museumAdapter)
            val newFavState = repository.isFavorite(userId, product.id)
            _uiState.value = _uiState.value.copy(isFavorite = newFavState)
        }
    }
}