package cl.duoc.museo.ui.viewmodels

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.model.User
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegistrationEvent {
    object Success : RegistrationEvent()
    data class Error(val message: String) : RegistrationEvent()
}

data class RegisterFormState(
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val sessionManager = SessionManager(application)

    private val _formState = MutableStateFlow(RegisterFormState())
    val formState: StateFlow<RegisterFormState> = _formState.asStateFlow()

    private val _selectedAvatar = MutableStateFlow("avatar_1")
    val selectedAvatar: StateFlow<String> = _selectedAvatar.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RegistrationEvent>()
    val eventFlow: SharedFlow<RegistrationEvent> = _eventFlow.asSharedFlow()

    fun selectAvatar(avatarId: String) {
        _selectedAvatar.value = avatarId
    }

    fun register(nameInput: String, emailInput: String, passwordInput: String) {
        val name = nameInput.trim()
        val email = emailInput.trim().lowercase()
        val password = passwordInput.trim()

        var isValid = true
        var nameErr: String? = null
        var emailErr: String? = null
        var passErr: String? = null

        if (name.isEmpty()) {
            nameErr = "El nombre completo es requerido"
            isValid = false
        }

        if (email.isEmpty()) {
            emailErr = "El correo electrónico es requerido"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailErr = "Ingresa un correo electrónico válido"
            isValid = false
        }

        if (password.isEmpty()) {
            passErr = "La contraseña es requerida"
            isValid = false
        } else if (password.length < 6) {
            passErr = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        _formState.value = RegisterFormState(
            nameError = nameErr,
            emailError = emailErr,
            passwordError = passErr,
        )

        if (!isValid) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true)

            try {
                val demoEmails = listOf(
                    "admin@museo.org",
                    "investigador@museo.org",
                    "cliente@museo.org",
                )
                if (email in demoEmails) {
                    _formState.value = _formState.value.copy(
                        emailError = "Este correo está reservado para un perfil de demostración",
                        isLoading = false,
                    )
                    _eventFlow.emit(
                        RegistrationEvent.Error("Usa un correo personal para crear una cuenta de cliente."),
                    )
                    return@launch
                }

                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    _formState.value = _formState.value.copy(
                        emailError = "Este correo electrónico ya está registrado",
                        isLoading = false,
                    )
                    _eventFlow.emit(RegistrationEvent.Error("El correo ya se encuentra registrado."))
                    return@launch
                }

                val newUser = User(
                    name = name,
                    email = email,
                    passwordHash = password,
                    role = Role.CLIENT,
                    avatarRes = _selectedAvatar.value,
                )

                val generatedId = userDao.insertUser(newUser)

                // Save user session in SessionManager to ensure data isolation
                sessionManager.saveUserSession(
                    userId = generatedId,
                    name = newUser.name,
                    email = newUser.email,
                    role = newUser.role,
                    avatar = newUser.avatarRes,
                )

                _formState.value = RegisterFormState(isLoading = false)
                _eventFlow.emit(RegistrationEvent.Success)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(isLoading = false)
                _eventFlow.emit(
                    RegistrationEvent.Error("Error al registrar usuario: ${e.localizedMessage ?: "inténtalo nuevamente."}"),
                )
            }
        }
    }
}