package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MuseumDetailUiState(
    val museum: Museum? = null,
    val isFavorite: Boolean = false,
    val userRole: Role = Role.CLIENT,
    val isLoading: Boolean = false,
    val publicationStatus: String = "Publicado",
)

class MuseumDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(MuseumDetailUiState())
    val uiState: StateFlow<MuseumDetailUiState> = _uiState.asStateFlow()

    private var currentMuseumId: String = "mus_01"

    fun init(museumId: String?) {
        val targetId = if (!museumId.isNullOrBlank()) museumId else "mus_01"
        currentMuseumId = targetId
        loadMuseumDetails(targetId)
    }

    fun loadMuseumDetails(museumId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val role = sessionManager.getUserRole()
            val userId = sessionManager.getUserId()

            // 1. Fetch from Room local cache database or backend API
            var museumData = repository.getMuseumById(museumId)

            if (museumData == null) {
                // Generate fallback mock details for colonial museum
                museumData = Museum(
                    id = museumId,
                    name = "Museo Nacional de Arte Colonial",
                    country = "Colombia",
                    project = "Arte Colonial Iberoamericano",
                    description = "Recorre siglos de historia a través de nuestras colecciones de arte colonial iberoamericano. Explora salas virtuales, exposiciones y material educativo.",
                    classification = "Historia / Arte Colonial",
                    imageUrl = "https://images.unsplash.com/photo-1565008447742-97f6f38c985c?w=500",
                    projectUrl = "https://museocolonial.gov.co/tour-virtual",
                    lat = 4.5960,
                    lng = -74.0750,
                )
            }

            // 2. Check favorite state specifically for the current logged-in user profile
            val favState = repository.isFavorite(userId, museumId)

            _uiState.value = MuseumDetailUiState(
                museum = museumData,
                isFavorite = favState,
                userRole = role,
                isLoading = false,
                publicationStatus = if (role == Role.ADMINISTRATOR) "Publicado (Verificado)" else "Publicado",
            )
        }
    }

    fun toggleFavorite() {
        val currentMuseum = _uiState.value.museum ?: return
        val userId = sessionManager.getUserId()

        viewModelScope.launch {
            repository.toggleFavorite(userId, currentMuseum)
            val newFavState = repository.isFavorite(userId, currentMuseum.id)
            _uiState.value = _uiState.value.copy(isFavorite = newFavState)
        }
    }

    fun approvePublication() {
        _uiState.value = _uiState.value.copy(publicationStatus = "Aprobado y Publicado")
    }

    fun rejectPublication() {
        _uiState.value = _uiState.value.copy(publicationStatus = "En Revisión (Incompleto)")
    }
}