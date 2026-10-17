package cl.duoc.museo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.museo.data.local.AppDatabase
import cl.duoc.museo.data.model.NotificationItem
import cl.duoc.museo.utils.ScanCodeResolver
import cl.duoc.museo.utils.ScannedContentKind
import cl.duoc.museo.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanResult {
    data class Success(
        val code: String,
        val contentId: String,
        val isMuseum: Boolean,
        val type: String,
        val title: String,
    ) : ScanResult()

    data class Error(val message: String) : ScanResult()
}

class QrScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)

    private val _scanResult = MutableSharedFlow<ScanResult>()
    val scanResult: SharedFlow<ScanResult> = _scanResult.asSharedFlow()

    private val _isTorchOn = MutableStateFlow(value = false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    @Volatile
    private var lastScannedCode: String? = null

    @Volatile
    private var scanCompleted = false

    @Volatile
    private var isProcessing = false

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    @Synchronized
    fun processScannedQrCode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) {
            viewModelScope.launch {
                _scanResult.emit(ScanResult.Error("Código QR o de barras vacío o no válido."))
            }
            return
        }
        if (code == lastScannedCode || scanCompleted || isProcessing) return
        lastScannedCode = code
        isProcessing = true

        viewModelScope.launch {
            try {
                val content = ScanCodeResolver.resolve(code)
                if (content == null) {
                    _scanResult.emit(
                        ScanResult.Error("El código no pertenece a un contenido registrado."),
                    )
                    isProcessing = false
                    return@launch
                }

                val userId = sessionManager.getUserId()
                val historyMessage = "Código procesado: $code (${content.type})"
                val notifications = db.notificationDao()
                if (!notifications.hasScannedCode(userId, historyMessage)) {
                    notifications.insertNotification(
                        NotificationItem(
                            userId = userId,
                            title = "Escáner: ${content.title}",
                            message = historyMessage,
                            category = "Escaner QR",
                            timestamp = "Hace un momento",
                        ),
                    )
                }

                scanCompleted = true
                _scanResult.emit(
                    ScanResult.Success(
                        code = code,
                        contentId = content.id,
                        isMuseum = content.kind == ScannedContentKind.MUSEUM,
                        type = content.type,
                        title = content.title,
                    ),
                )
            } catch (exception: Exception) {
                lastScannedCode = null
                _scanResult.emit(
                    ScanResult.Error(
                        "No se pudo guardar el escaneo: ${exception.localizedMessage ?: "inténtalo nuevamente."}",
                    ),
                )
            } finally {
                isProcessing = false
            }
        }
    }

    fun resetLastScannedCode() {
        lastScannedCode = null
        scanCompleted = false
        isProcessing = false
    }
}
