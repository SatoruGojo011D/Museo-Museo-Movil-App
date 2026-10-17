package cl.duoc.museo.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.MuseumMapItem
import cl.duoc.museo.data.remote.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val _allMuseums = MutableStateFlow<List<MuseumMapItem>>(emptyList())
    private val _filteredMuseums = MutableStateFlow<List<MuseumMapItem>>(emptyList())
    val museums: StateFlow<List<MuseumMapItem>> = _filteredMuseums.asStateFlow()

    private val _selectedMuseum = MutableStateFlow<MuseumMapItem?>(null)
    val selectedMuseum: StateFlow<MuseumMapItem?> = _selectedMuseum.asStateFlow()

    private val _activeFilter = MutableStateFlow("Todos")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    init {
        loadMuseumsData()
    }

    fun loadMuseumsData() {
        viewModelScope.launch {
            try {
                val mockNetworkData = fetchFromNetworkWithLocalCacheFallback()
                _allMuseums.value = mockNetworkData
                _filteredMuseums.value = mockNetworkData
                _selectedMuseum.value = mockNetworkData.firstOrNull()
            } catch (e: Exception) {
                Log.e("MapViewModel", "No se pudieron cargar los museos del mapa.", e)
                val fallbackData = getMockMuseumList()
                _allMuseums.value = fallbackData
                _filteredMuseums.value = fallbackData
                _selectedMuseum.value = fallbackData.firstOrNull()
            }
        }
    }

    private suspend fun fetchFromNetworkWithLocalCacheFallback(): List<MuseumMapItem> {
        return try {
            val response = ApiClient.apiService.getVirtualMuseums()
            val museums = response.body()
            if (response.isSuccessful && !museums.isNullOrEmpty()) {
                db.museumDao().insertAll(museums)
                museums.map(::toMapItem)
            } else {
                getCachedOrDemoMuseums()
            }
        } catch (e: Exception) {
            Log.w("MapViewModel", "No se pudo actualizar el mapa desde el servicio; se usará la caché local.", e)
            getCachedOrDemoMuseums()
        }
    }

    private suspend fun getCachedOrDemoMuseums(): List<MuseumMapItem> {
        val cachedMuseums = db.museumDao().getAllMuseums().first()
        if (cachedMuseums.isNotEmpty()) return cachedMuseums.map(::toMapItem)

        val demoMuseums = getMockMuseumList()
        db.museumDao().insertAll(demoMuseums.map { museum ->
            Museum(
                id = museum.id,
                name = museum.title,
                country = museum.country,
                project = museum.category,
                description = "",
                classification = museum.category,
                imageUrl = museum.imageUrl,
                projectUrl = museum.virtualTourUrl,
                lat = museum.lat,
                lng = museum.lng,
            )
        })
        return demoMuseums
    }

    private fun toMapItem(museum: Museum) = MuseumMapItem(
        id = museum.id,
        title = museum.name,
        location = museum.country,
        country = museum.country.substringAfterLast(", ").trim(),
        category = museum.classification,
        lat = museum.lat,
        lng = museum.lng,
        virtualTourUrl = museum.projectUrl,
        imageUrl = museum.imageUrl,
        rating = 4.8,
        reviewsCount = 120,
        tag1 = museum.classification.ifBlank { "Historia" },
        tag2 = "Virtual",
    )

    fun applyFilter(filterName: String) {
        _activeFilter.value = filterName
        val currentList = _allMuseums.value

        val filtered = when (filterName) {
            "Todos" -> currentList
            else -> when {
                filterName.startsWith("country:") -> {
                    val country = filterName.removePrefix("country:")
                    currentList.filter { it.country.equals(country, ignoreCase = true) }
                }
                filterName.startsWith("project:") -> {
                    val project = filterName.removePrefix("project:")
                    currentList.filter { it.category.equals(project, ignoreCase = true) }
                }
                else -> currentList
            }
        }
        _filteredMuseums.value = filtered
        val selectedId = _selectedMuseum.value?.id
        if (selectedId != null && filtered.none { it.id == selectedId }) {
            _selectedMuseum.value = filtered.firstOrNull()
        }
    }

    fun selectMuseum(museum: MuseumMapItem) {
        _selectedMuseum.value = museum
    }

    fun clearSelection() {
        _selectedMuseum.value = null
    }

    private fun getMockMuseumList(): List<MuseumMapItem> {
        return listOf(
            MuseumMapItem(
                id = "mus_01",
                title = "Museo Nacional\nde Arte Colonial",
                location = "Bogotá, Colombia",
                category = "Arte Colonial",
                lat = 4.5960,
                lng = -74.0750,
                virtualTourUrl = "https://museocolonial.gov.co/tour-virtual-3d",
                imageUrl = "https://images.unsplash.com/photo-1565008447742-97f6f38c985c?w=500",
                rating = 4.8,
                reviewsCount = 120,
                tag1 = "Historia",
                tag2 = "Virtual",
                country = "Colombia",
            ),
            MuseumMapItem(
                id = "mus_02",
                title = "Museo del Prado\nIberoamérica",
                location = "Madrid, España",
                category = "Bellas Artes",
                lat = 40.4138,
                lng = -3.6921,
                virtualTourUrl = "https://museodelprado.es/recorrido-virtual",
                imageUrl = "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=500",
                rating = 4.9,
                reviewsCount = 350,
                tag1 = "Arte",
                tag2 = "Virtual 3D",
                country = "España",
            ),
            MuseumMapItem(
                id = "mus_03",
                title = "Museo Arqueológico\nLarco",
                location = "Lima, Perú",
                category = "Arqueología",
                lat = -12.0722,
                lng = -77.0708,
                virtualTourUrl = "https://museolarco.org/tour-virtual",
                imageUrl = "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=500",
                rating = 4.7,
                reviewsCount = 210,
                tag1 = "Precolombino",
                tag2 = "Virtual",
                country = "Perú",
            ),
            MuseumMapItem(
                id = "mus_04",
                title = "Museo Soumaya\nPlaza Carso",
                location = "Ciudad de México, México",
                category = "Arte Moderno",
                lat = 19.4407,
                lng = -99.2047,
                virtualTourUrl = "https://museosoumaya.org/3d-experience",
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500",
                rating = 4.8,
                reviewsCount = 185,
                tag1 = "Escultura",
                tag2 = "Virtual",
                country = "México",
            ),
            MuseumMapItem(
                id = "mus_05",
                title = "Museo Histórico\nNacional",
                location = "Santiago, Chile",
                category = "Historia",
                lat = -33.4372,
                lng = -70.6506,
                virtualTourUrl = "https://mhn.gob.cl/recorrido-virtual",
                imageUrl = "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=500",
                rating = 4.6,
                reviewsCount = 98,
                tag1 = "Patrimonio",
                tag2 = "Virtual",
                country = "Chile",
            ),
        )
    }
}