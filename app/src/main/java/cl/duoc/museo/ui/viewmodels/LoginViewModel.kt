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

sealed class LoginEvent {
    object Success : LoginEvent()
    data class Error(val message: String) : LoginEvent()
    data class Info(val message: String) : LoginEvent()
}

data class LoginFormState(
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val sessionManager = SessionManager(application)

    private val _selectedRole = MutableStateFlow(Role.CLIENT)
    val selectedRole: StateFlow<Role> = _selectedRole.asStateFlow()

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginEvent>()
    val eventFlow: SharedFlow<LoginEvent> = _eventFlow.asSharedFlow()

    fun selectRole(role: Role) {
        _selectedRole.value = role
    }

    fun getTestCredentialsForRole(role: Role): Pair<String, String> {
        return when (role) {
            Role.ADMINISTRATOR -> Pair("admin@museo.org", "admin123")
            Role.RESEARCHER -> Pair("investigador@museo.org", "investiga123")
            Role.CLIENT -> Pair("cliente@museo.org", "cliente123")
        }
    }

    fun login(emailInput: String, passwordInput: String) {
        val email = emailInput.trim().lowercase()
        val password = passwordInput.trim()

        var isValid = true
        var emailErr: String? = null
        var passErr: String? = null

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
        }

        _formState.value = LoginFormState(
            emailError = emailErr,
            passwordError = passErr,
        )

        if (!isValid) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true)

            try {
                val role = _selectedRole.value
                val demoCredentials = getTestCredentialsForRole(role)
                val isDemoAccount = email == demoCredentials.first
                val existingUser = userDao.getUserByEmail(email)
                val credentialsMatch = when {
                    isDemoAccount ->
                        password == demoCredentials.second &&
                            (existingUser == null || existingUser.role == role)
                    existingUser != null ->
                        role == Role.CLIENT &&
                            existingUser.role == role &&
                            existingUser.passwordHash == password
                    else -> false
                }

                if (!credentialsMatch) {
                    _formState.value = LoginFormState(isLoading = false)
                    _eventFlow.emit(LoginEvent.Error("Correo, contraseña o perfil incorrectos."))
                    return@launch
                }

                val defaultName = when (role) {
                    Role.ADMINISTRATOR -> "Administrador Demo"
                    Role.RESEARCHER -> "Dra. Elena Ramos"
                    Role.CLIENT -> "María Fernanda López"
                }
                val avatar = existingUser?.avatarRes
                    ?: if (role == Role.ADMINISTRATOR) "avatar_2" else "avatar_1"
                val userId = existingUser?.id ?: if (isDemoAccount) {
                    userDao.insertUser(
                        User(
                            name = defaultName,
                            email = email,
                            passwordHash = demoCredentials.second,
                            role = role,
                            avatarRes = avatar,
                        ),
                    )
                } else {
                    0L
                }
                val userName = existingUser?.name ?: defaultName

                sessionManager.saveUserSession(
                    userId = userId,
                    name = userName,
                    email = email,
                    role = role,
                    avatar = avatar,
                )

                _formState.value = LoginFormState(isLoading = false)
                _eventFlow.emit(LoginEvent.Success)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(isLoading = false)
                _eventFlow.emit(
                    LoginEvent.Error("Error al iniciar sesión: ${e.localizedMessage ?: "inténtalo nuevamente."}"),
                )
            }
        }
    }

    fun requestPasswordReset(emailInput: String) {
        val email = emailInput.trim()
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _formState.value = _formState.value.copy(emailError = "Ingresa un correo válido para recuperar la contraseña")
            return
        }

        viewModelScope.launch {
            _eventFlow.emit(LoginEvent.Info("Se han enviado las instrucciones de recuperación a $email"))
        }
    }
}