package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _userRole = MutableStateFlow(Role.CLIENT)
    val userRole: StateFlow<Role> = _userRole.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _rawNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val _filteredNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notificationsList: StateFlow<List<NotificationItem>> = _filteredNotifications.asStateFlow()

    private val _isFabVisible = MutableStateFlow(value = false)
    val isFabVisible: StateFlow<Boolean> = _isFabVisible.asStateFlow()

    private val _isEmpty = MutableStateFlow(value = false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val role = sessionManager.getUserRole()
        val userId = sessionManager.getUserId()
        _userRole.value = role
        _isFabVisible.value = (role == Role.ADMINISTRATOR)

        viewModelScope.launch {
            seedInitialNotificationsIfEmpty(userId)

            repository.getNotificationsForUser(userId).collectLatest { list ->
                _rawNotifications.value = list
                applyCategoryFilter()
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
        applyCategoryFilter()
    }

    private fun applyCategoryFilter() {
        val category = _selectedCategory.value
        val raw = _rawNotifications.value

        val filtered = if (category.equals("Todas", ignoreCase = true)) {
            raw
        } else {
            raw.filter { notif ->
                notif.category.equals(category, ignoreCase = true)
            }
        }

        _filteredNotifications.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun markAsRead(item: NotificationItem) {
        viewModelScope.launch {
            repository.markNotificationAsRead(item.id)
        }
    }

    fun sendAdminBroadcast(title: String, message: String, category: String) {
        val userId = sessionManager.getUserId()
        viewModelScope.launch {
            repository.addNotification(
                NotificationItem(
                    userId = userId,
                    title = title,
                    message = message,
                    category = category,
                    timestamp = "Ahora",
                    isRead = false,
                ),
            )
        }
    }

    private suspend fun seedInitialNotificationsIfEmpty(userId: Long) {
        val mockNotifs = listOf(
            NotificationItem(
                userId = userId,
                title = "Nuevo curso disponible",
                message = "Curso de Museología Digital e Innovación",
                category = "Cursos",
                timestamp = "10 min",
                isRead = false,
            ),
            NotificationItem(
                userId = userId,
                title = "Nuevo museo virtual",
                message = "Museo de Arte Contemporáneo de Quito",
                category = "Museos",
                timestamp = "1 h",
                isRead = false,
            ),
            NotificationItem(
                userId = userId,
                title = "Estado de tu pedido",
                message = "Tu pedido #2024-00122 ha sido enviado",
                category = "Pedidos",
                timestamp = "3 h",
                isRead = true,
            ),
            NotificationItem(
                userId = userId,
                title = "Nueva publicación",
                message = "El Arte Colonial en Iberoamérica",
                category = "Publicaciones",
                timestamp = "1 dia",
                isRead = true,
            ),
            NotificationItem(
                userId = userId,
                title = "Evento próximo",
                message = "Charla en vivo: Museos y Comunidades",
                category = "Eventos",
                timestamp = "2 dias",
                isRead = true,
            ),
            NotificationItem(
                userId = userId,
                title = "Recordatorio",
                message = "Tienes 2 cursos pendientes de revisión",
                category = "Cursos",
                timestamp = "3 dias",
                isRead = true,
            ),
        )

        mockNotifs.forEach { notif ->
            repository.addNotification(notif)
        }
    }
}