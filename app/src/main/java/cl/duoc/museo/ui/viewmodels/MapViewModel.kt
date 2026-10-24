package cl.duoc.museo.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.Museum
import cl.duoc.museo.data.model.MuseumMapItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            val chileanMuseums = getChileanMuseumList()
            _allMuseums.value = chileanMuseums
            _filteredMuseums.value = chileanMuseums
            _selectedMuseum.value = chileanMuseums.firstOrNull()

            try {
                db.museumDao().insertAll(chileanMuseums.map { museum ->
                    Museum(
                        id = museum.id,
                        name = museum.title,
                        country = museum.location,
                        project = museum.category,
                        description = "",
                        classification = museum.category,
                        imageUrl = museum.imageUrl,
                        projectUrl = museum.virtualTourUrl,
                        lat = museum.lat,
                        lng = museum.lng,
                    )
                })
            } catch (exception: Exception) {
                Log.e("MapViewModel", "No se pudieron guardar los museos chilenos en la caché.", exception)
            }
        }
    }

    fun applyFilter(filterName: String) {
        _activeFilter.value = filterName
        val currentList = _allMuseums.value

        val filtered = when (filterName) {
            "Todos" -> currentList
            else -> filterName
                .takeIf { it.startsWith("project:") }
                ?.removePrefix("project:")
                ?.let { project -> currentList.filter { it.category.equals(project, ignoreCase = true) } }
                ?: currentList
        }
        _filteredMuseums.value = filtered
        val selectedId = _selectedMuseum.value?.id
        if (selectedId != null && filtered.none { it.id == selectedId }) {
            _selectedMuseum.value = filtered.firstOrNull()
        }
    }

    fun availableProjects(): List<String> =
        _allMuseums.value.map { it.category }.distinct().sorted()

    fun selectMuseum(museum: MuseumMapItem) {
        _selectedMuseum.value = museum
    }

    fun clearSelection() {
        _selectedMuseum.value = null
    }

    private fun getChileanMuseumList(): List<MuseumMapItem> {
        return listOf(
            MuseumMapItem(
                id = "mus_01",
                title = "Museo Arqueológico\nR. P. Gustavo Le Paige",
                location = "San Pedro de Atacama, Chile",
                category = "Arqueología",
                lat = -22.9111,
                lng = -68.2014,
                virtualTourUrl = "https://www.museoarq.cl/",
                imageUrl = "https://images.unsplash.com/photo-1565008447742-97f6f38c985c?w=500",
                rating = 4.8,
                reviewsCount = 186,
                tag1 = "Atacama",
                tag2 = "Virtual",
                country = "Chile",
            ),
            MuseumMapItem(
                id = "mus_02",
                title = "Museo Gabriela Mistral\nde Vicuña",
                location = "Vicuña, Chile",
                category = "Literatura",
                lat = -30.0318,
                lng = -70.7081,
                virtualTourUrl = "https://www.mgmistral.gob.cl/",
                imageUrl = "https://images.unsplash.com/photo-1582555172866-f73bb12a2ab3?w=500",
                rating = 4.7,
                reviewsCount = 142,
                tag1 = "Literatura",
                tag2 = "Patrimonio",
                country = "Chile",
            ),
            MuseumMapItem(
                id = "mus_03",
                title = "Museo de Historia Natural\nde Valparaíso",
                location = "Valparaíso, Chile",
                category = "Ciencias Naturales",
                lat = -33.0451,
                lng = -71.6197,
                virtualTourUrl = "https://www.mhnv.gob.cl/",
                imageUrl = "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=500",
                rating = 4.6,
                reviewsCount = 104,
                tag1 = "Naturaleza",
                tag2 = "Virtual",
                country = "Chile",
            ),
            MuseumMapItem(
                id = "mus_04",
                title = "Museo Nacional\nde Bellas Artes",
                location = "Santiago, Chile",
                category = "Bellas Artes",
                lat = -33.4352,
                lng = -70.6436,
                virtualTourUrl = "https://www.mnba.gob.cl/",
                imageUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500",
                rating = 4.8,
                reviewsCount = 265,
                tag1 = "Arte",
                tag2 = "Virtual",
                country = "Chile",
            ),
            MuseumMapItem(
                id = "mus_05",
                title = "Museo Regional\nde Magallanes",
                location = "Punta Arenas, Chile",
                category = "Historia",
                lat = -53.1638,
                lng = -70.9062,
                virtualTourUrl = "https://www.museodemagallanes.gob.cl/",
                imageUrl = "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=500",
                rating = 4.7,
                reviewsCount = 91,
                tag1 = "Magallanes",
                tag2 = "Virtual",
                country = "Chile",
            ),
        )
    }
}