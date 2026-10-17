package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Favorite
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MuseumRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)

    private val _activeFilter = MutableStateFlow("MUSEOS") // "MUSEOS" vs "RECURSOS"
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    private val _allFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val _displayedFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favoritesList: StateFlow<List<Favorite>> = _displayedFavorites.asStateFlow()

    private val _museumsCount = MutableStateFlow(0)
    val museumsCount: StateFlow<Int> = _museumsCount.asStateFlow()

    private val _resourcesCount = MutableStateFlow(0)
    val resourcesCount: StateFlow<Int> = _resourcesCount.asStateFlow()

    private val _isEmpty = MutableStateFlow(value = false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    init {
        loadUserFavorites()
    }

    fun loadUserFavorites() {
        val userId = sessionManager.getUserId()

        viewModelScope.launch {
            // Seed initial favorites for demo client profile if database is empty
            seedInitialFavoritesIfEmpty(userId)

            // CRITICAL REQUIREMENT: Flow query filtered by current active profile ID to maintain data separation
            repository.getFavoritesForUser(userId).collectLatest { list ->
                _allFavorites.value = list
                updateCountsAndFilter(list)
            }
        }
    }

    fun setFilter(filterType: String) {
        _activeFilter.value = filterType
        updateCountsAndFilter(_allFavorites.value)
    }

    private fun updateCountsAndFilter(list: List<Favorite>) {
        val museums = list.filter { fav ->
            !fav.classification.contains("Recurso", ignoreCase = true) &&
                !fav.classification.contains("Libro", ignoreCase = true)
        }
        val resources = list.filter { fav ->
            fav.classification.contains("Recurso", ignoreCase = true) ||
                fav.classification.contains("Libro", ignoreCase = true)
        }

        _museumsCount.value = museums.size
        _resourcesCount.value = resources.size

        val currentFilter = _activeFilter.value
        val filtered = if (currentFilter == "RECURSOS") resources else museums
        _displayedFavorites.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    fun removeFavorite(favorite: Favorite) {
        val userId = sessionManager.getUserId()
        viewModelScope.launch {
            val museum = Museum(
                id = favorite.museumId,
                name = favorite.museumName,
                country = favorite.museumCountry,
                project = favorite.classification,
                description = "",
                classification = favorite.classification,
                imageUrl = favorite.imageUrl,
                projectUrl = "",
                lat = 0.0,
                lng = 0.0,
            )
            repository.toggleFavorite(userId, museum)
        }
    }

    private suspend fun seedInitialFavoritesIfEmpty(userId: Long) {
        val initialMockFavorites = listOf(
            Favorite(
                userId = userId,
                museumId = "mus_01",
                museumName = "Museo Nacional de Arte Colonial",
                museumCountry = "Bogotá, Colombia",
                imageUrl = "https://images.unsplash.com/photo-1565008447742-97f6f38c985c?w=500",
                classification = "Arte Colonial",
            ),
            Favorite(
                userId = userId,
                museumId = "mus_02",
                museumName = "Museo de Arte Moderno",
                museumCountry = "Ciudad de México, México",
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500",
                classification = "Arte Moderno",
            ),
            Favorite(
                userId = userId,
                museumId = "mus_03",
                museumName = "Museo del Prado Virtual",
                museumCountry = "Madrid, España",
                imageUrl = "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=500",
                classification = "Bellas Artes",
            ),
            Favorite(
                userId = userId,
                museumId = "mus_04",
                museumName = "Museo de Arte de Lima",
                museumCountry = "Lima, Perú",
                imageUrl = "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=500",
                classification = "Arqueología",
            ),
            Favorite(
                userId = userId,
                museumId = "mus_05",
                museumName = "Museo Nacional de Bellas Artes",
                museumCountry = "Santiago, Chile",
                imageUrl = "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=500",
                classification = "Historia",
            ),
            Favorite(
                userId = userId,
                museumId = "cat_02",
                museumName = "El Arte Colonial en Iberoamérica",
                museumCountry = "Publicación Digital",
                imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500",
                classification = "Libro / Recurso",
            ),
            Favorite(
                userId = userId,
                museumId = "cat_05",
                museumName = "Catálogo Conmemorativo Soumaya 3D",
                museumCountry = "Recurso Educativo",
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500",
                classification = "Recurso",
            ),
            Favorite(
                userId = userId,
                museumId = "cat_06",
                museumName = "Guía Técnica de Digitalización",
                museumCountry = "Manual Técnico",
                imageUrl = "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=500",
                classification = "Recurso",
            ),
        )

        initialMockFavorites.forEach { fav ->
            val exists = repository.isFavorite(userId, fav.museumId)
            if (!exists) {
                repository.toggleFavorite(
                    userId,
                    Museum(
                        id = fav.museumId,
                        name = fav.museumName,
                        country = fav.museumCountry,
                        project = fav.classification,
                        description = "",
                        classification = fav.classification,
                        imageUrl = fav.imageUrl,
                        projectUrl = "",
                        lat = 0.0,
                        lng = 0.0,
                    ),
                )
            }
        }
    }
}