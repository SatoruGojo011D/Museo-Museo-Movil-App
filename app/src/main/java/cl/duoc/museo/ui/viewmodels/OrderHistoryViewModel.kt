package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.data.model.Order
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class OrderHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _userRole = MutableStateFlow(Role.CLIENT)
    val userRole: StateFlow<Role> = _userRole.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("Todas")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _rawOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val ordersList: StateFlow<List<Order>> = _filteredOrders.asStateFlow()

    private val _isEmpty = MutableStateFlow(value = false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()
    private var ordersCollectionJob: Job? = null

    init {
        loadOrders()
    }

    fun loadOrders() {
        val role = sessionManager.getUserRole()
        val userId = sessionManager.getUserId()
        _userRole.value = role

        ordersCollectionJob?.cancel()
        ordersCollectionJob = viewModelScope.launch {
            val flow = if (role == Role.ADMINISTRATOR) {
                repository.getAllOrders()
            } else {
                repository.getOrdersForUser(userId)
            }

            flow.collectLatest { list ->
                _rawOrders.value = list
                applyStatusFilter()
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _selectedStatusFilter.value = filter
        applyStatusFilter()
    }

    private fun applyStatusFilter() {
        val currentFilter = _selectedStatusFilter.value
        val raw = _rawOrders.value

        val filtered = when (currentFilter.lowercase()) {
            "pendientes" -> raw.filter {
                it.status.equals("Pendiente", ignoreCase = true) ||
                    it.status.equals("Confirmado", ignoreCase = true)
            }
            "enviadas" -> raw.filter {
                it.status.equals("Enviado", ignoreCase = true) ||
                    it.status.equals("En camino", ignoreCase = true)
            }
            "entregadas" -> raw.filter {
                it.status.equals("Entregado", ignoreCase = true)
            }
            else -> raw
        }

        _filteredOrders.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, newStatus)

            // Notify user of order status change
            val formattedId = "#2024-${order.id.toString().padStart(5, '0')}"
            repository.addNotification(
                NotificationItem(
                    userId = order.userId,
                    title = "Estado Actualizado: Solicitud $formattedId",
                    message = "Tu pedido $formattedId ha cambiado al estado '$newStatus'.",
                    category = "Pedidos",
                    timestamp = "Hace un momento",
                ),
            )
        }
    }

}