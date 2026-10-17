package cl.duoc.museo.ui.viewmodels

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "María Fernanda López",
    val email: String = "maria@email.com",
    val role: Role = Role.CLIENT,
    val avatarRes: String = "avatar_1",
    val isClientOptionsVisible: Boolean = true,
    val language: String = "Español",
    val shippingAddress: String = "",
    val interests: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = true,
    val largeTextEnabled: Boolean = false,
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent: SharedFlow<String> = _messageEvent.asSharedFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val email = sessionManager.getUserEmail()
        val sessionName = sessionManager.getUserName()
        val role = sessionManager.getUserRole()

        viewModelScope.launch {
            val dbUser = repository.getUserByEmail(email)
            val name = dbUser?.name ?: sessionName
            val avatar = dbUser?.avatarRes ?: "avatar_1"

            // Client role exclusive options ("Direcciones de envío", "Mis intereses")
            val isClient = (role == Role.CLIENT)

            _uiState.value = ProfileUiState(
                name = name,
                email = email,
                role = role,
                avatarRes = avatar,
                isClientOptionsVisible = isClient,
                language = sessionManager.getLanguage(),
                shippingAddress = sessionManager.getShippingAddress(),
                interests = sessionManager.getInterests(),
                notificationsEnabled = sessionManager.areNotificationsEnabled(),
                largeTextEnabled = sessionManager.isLargeTextEnabled(),
            )
        }
    }

    fun updatePersonalData(nameInput: String, emailInput: String) {
        val name = nameInput.trim()
        val email = emailInput.trim().lowercase()
        if (name.isBlank()) {
            emitMessage("El nombre completo es obligatorio.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emitMessage("Ingresa un correo electrónico válido.")
            return
        }

        viewModelScope.launch {
            try {
                val currentEmail = sessionManager.getUserEmail()
                val demoEmails = listOf(
                    "admin@museo.org",
                    "investigador@museo.org",
                    "cliente@museo.org",
                )
                if (currentEmail in demoEmails && email != currentEmail) {
                    _messageEvent.emit("El correo de un perfil de demostración no se puede cambiar.")
                    return@launch
                }

                val userId = sessionManager.getUserId()
                val duplicate = repository.getUserByEmail(email)
                if (duplicate != null && duplicate.id != userId) {
                    _messageEvent.emit("Ese correo ya está asociado a otra cuenta.")
                    return@launch
                }

                val user = repository.getUserByEmail(currentEmail)
                if (user != null) {
                    repository.updateUser(user.copy(name = name, email = email))
                }
                val state = _uiState.value
                sessionManager.saveUserSession(userId, name, email, state.role, state.avatarRes)
                _uiState.value = state.copy(name = name, email = email)
                _messageEvent.emit("Perfil actualizado correctamente.")
            } catch (exception: Exception) {
                _messageEvent.emit(
                    "No se pudo actualizar el perfil: ${exception.localizedMessage ?: "inténtalo de nuevo."}",
                )
            }
        }
    }

    fun updateShippingAddress(addressInput: String) {
        val address = addressInput.trim()
        if (address.isBlank()) {
            emitMessage("Ingresa una dirección antes de guardar.")
            return
        }
        sessionManager.saveShippingAddress(address)
        _uiState.value = _uiState.value.copy(shippingAddress = address)
        emitMessage("Dirección guardada.")
    }

    fun updateInterests(interests: Set<String>) {
        sessionManager.saveInterests(interests)
        _uiState.value = _uiState.value.copy(interests = interests)
        emitMessage("Preferencias de interés guardadas.")
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sessionManager.setNotificationsEnabled(enabled)
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        emitMessage(if (enabled) "Notificaciones activadas." else "Notificaciones desactivadas.")
    }

    fun setLargeTextEnabled(enabled: Boolean) {
        sessionManager.setLargeTextEnabled(enabled)
        _uiState.value = _uiState.value.copy(largeTextEnabled = enabled)
        emitMessage(if (enabled) "Preferencia de texto grande guardada." else "Tamaño de texto estándar guardado.")
    }

    fun setLanguage(language: String) {
        sessionManager.setLanguage(language)
        _uiState.value = _uiState.value.copy(language = language)
        emitMessage("Idioma guardado: $language.")
    }

    fun cycleAvatar() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val nextAvatar = when (state.avatarRes) {
                    "avatar_1" -> "avatar_2"
                    "avatar_2" -> "avatar_3"
                    "avatar_3" -> "avatar_4"
                    else -> "avatar_1"
                }
                val user = repository.getUserByEmail(state.email)
                if (user != null) repository.updateUser(user.copy(avatarRes = nextAvatar))
                sessionManager.saveUserSession(
                    sessionManager.getUserId(),
                    state.name,
                    state.email,
                    state.role,
                    nextAvatar,
                )
                _uiState.value = state.copy(avatarRes = nextAvatar)
            } catch (exception: Exception) {
                _messageEvent.emit(
                    "No se pudo actualizar el avatar: ${exception.localizedMessage ?: "inténtalo de nuevo."}",
                )
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messageEvent.emit(message)
        }
    }
}