package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Maria",
    val greetingText: String = "¡Hola, Maria!",
    val subtitleText: String = "Sigamos explorando\nel conocimiento del mundo.",
    val role: Role = Role.CLIENT,
    val avatarResId: String = "avatar_1",
    val hasUnreadNotifications: Boolean = true,
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            val name = sessionManager.getUserName()
            val role = sessionManager.getUserRole()
            val avatar = sessionManager.getUserAvatar()
            val firstName = if (name.contains(" ")) name.substringBefore(" ") else name

            // Attempt to load fresh data from DB if available
            val userId = sessionManager.getUserId()
            val userFromDb = userDao.getUserById(userId)
            val hasUnreadNotifications = database.notificationDao()
                .getUnreadNotificationCount(userId) > 0

            val finalName = userFromDb?.name?.substringBefore(" ") ?: firstName

            _uiState.value = DashboardUiState(
                userName = finalName,
                greetingText = "¡Hola, $finalName!",
                subtitleText = getSubtitleForRole(role),
                role = role,
                avatarResId = userFromDb?.avatarRes ?: avatar,
                hasUnreadNotifications = hasUnreadNotifications,
            )
        }
    }

    private fun getSubtitleForRole(role: Role): String {
        return when (role) {
            Role.CLIENT -> "Sigamos explorando\nel conocimiento del mundo."
            Role.RESEARCHER -> "Panel de investigación y\nrevisión de proyectos."
            Role.ADMINISTRATOR -> "Panel de administración y\ngestión global del sistema."
        }
    }
}