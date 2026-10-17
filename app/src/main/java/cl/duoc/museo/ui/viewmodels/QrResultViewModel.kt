package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duoc.museo.utils.ScanCodeResolver
import cl.duoc.museo.utils.ScannedContentKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QrResultUiState(
    val isValid: Boolean = false,
    val code: String = "",
    val type: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val targetId: String = "",
    val isMuseum: Boolean = false,
    val errorMessage: String = "",
)

class QrResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(QrResultUiState())
    val uiState: StateFlow<QrResultUiState> = _uiState.asStateFlow()

    fun resolveScannedCode(scannedCode: String?) {
        val code = scannedCode?.trim().orEmpty()
        val content = ScanCodeResolver.resolve(code)
        if (content == null) {
            _uiState.value = QrResultUiState(
                isValid = false,
                code = code,
                errorMessage = "El código QR escaneado no pertenece a un contenido registrado o expiró.",
            )
            return
        }

        _uiState.value = QrResultUiState(
            isValid = true,
            code = code,
            type = content.type,
            title = content.title,
            author = content.author,
            description = content.description,
            targetId = content.id,
            isMuseum = content.kind == ScannedContentKind.MUSEUM,
        )
    }
}
