package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.CartItem
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class CheckoutEvent {
    data class Success(val orderTotal: Double) : CheckoutEvent()
    data class Error(val message: String) : CheckoutEvent()
}

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _shippingCost = MutableStateFlow(12.0)
    val shippingCost: StateFlow<Double> = _shippingCost.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _selectedCountry = MutableStateFlow("Chile")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _selectedShippingType = MutableStateFlow("Estándar (7-14 días)")
    val selectedShippingType: StateFlow<String> = _selectedShippingType.asStateFlow()

    private val _checkoutEvent = MutableSharedFlow<CheckoutEvent>()
    val checkoutEvent: SharedFlow<CheckoutEvent> = _checkoutEvent.asSharedFlow()
    private var cartCollectionJob: Job? = null

    init {
        loadUserCart()
    }

    fun loadUserCart() {
        if (cartCollectionJob?.isActive == true) return
        val userId = sessionManager.getUserId()
        cartCollectionJob = viewModelScope.launch {
            repository.getCartItemsForUser(userId).collectLatest { items ->
                _cartItems.value = items
                recalculateTotals(items)
            }
        }
    }

    fun selectCountry(country: String) {
        _selectedCountry.value = country
        recalculateTotals(_cartItems.value)
    }

    fun selectShippingType(shippingType: String) {
        _selectedShippingType.value = shippingType
        recalculateTotals(_cartItems.value)
    }

    private fun recalculateTotals(items: List<CartItem>) {
        val sub = items.sumOf { it.price * it.quantity }
        _subtotal.value = sub

        val country = _selectedCountry.value
        val isNational = country.equals("Chile", ignoreCase = true)
        val isExpress = _selectedShippingType.value.contains("Expreso", ignoreCase = true)

        val cost = when {
            isNational && isExpress -> 18.0
            isNational -> 12.0
            isExpress -> 38.0
            else -> 25.0
        }

        _shippingCost.value = if (items.isEmpty()) 0.0 else cost
        _totalAmount.value = if (items.isEmpty()) 0.0 else (sub + _shippingCost.value)
    }

    fun incrementQuantity(item: CartItem) {
        viewModelScope.launch {
            repository.updateCartQuantity(item.id, item.quantity + 1)
        }
    }

    fun decrementQuantity(item: CartItem) {
        viewModelScope.launch {
            if (item.quantity > 1) {
                repository.updateCartQuantity(item.id, item.quantity - 1)
            } else {
                repository.removeFromCart(item.id)
            }
        }
    }

    fun deleteItem(item: CartItem) {
        viewModelScope.launch {
            repository.removeFromCart(item.id)
        }
    }

    fun checkout() {
        val items = _cartItems.value
        if (items.isEmpty()) {
            viewModelScope.launch {
                _checkoutEvent.emit(CheckoutEvent.Error("El carrito está vacío."))
            }
            return
        }

        val userId = sessionManager.getUserId()
        val country = _selectedCountry.value
        val shippingType = _selectedShippingType.value

        viewModelScope.launch {
            try {
                val orderTotal = repository.checkout(userId, country, shippingType)
                if (orderTotal != null) {
                    _checkoutEvent.emit(CheckoutEvent.Success(orderTotal))
                } else {
                    _checkoutEvent.emit(CheckoutEvent.Error("No se pudo procesar la solicitud."))
                }
            } catch (e: Exception) {
                _checkoutEvent.emit(CheckoutEvent.Error("Error en el pago: ${e.localizedMessage}"))
            }
        }
    }

}