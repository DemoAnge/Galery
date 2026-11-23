package com.example.galeria.features.vault.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.features.vault.domain.ChangeLockUseCase
import com.example.galeria.features.vault.domain.DeleteFromVaultUseCase
import com.example.galeria.features.vault.domain.ExportFromVaultUseCase
import com.example.galeria.features.vault.domain.LockMethod
import com.example.galeria.features.vault.domain.SetupLockUseCase
import com.example.galeria.features.vault.domain.UnlockVaultUseCase
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.domain.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed class VaultUiState {
    data object Loading : VaultUiState()
    data object NeedsSetup : VaultUiState()
    data class Locked(
        val method: LockMethod,
        val biometricAvailable: Boolean,
        val biometricEnabled: Boolean,
        val pinFallback: Boolean = false,
        val busy: Boolean = false,
        val error: String? = null,
        val lockoutRemainingMs: Long = 0,
    ) : VaultUiState()
    data class Unlocked(
        val items: List<VaultItem>,
        val offerBiometric: Boolean = false,
        val message: String? = null,
        val openedCollection: String? = null,
    ) : VaultUiState()
    data class ChangingLock(
        val currentMethod: LockMethod,
        val items: List<VaultItem>,
        val openedCollection: String? = null,
        val busy: Boolean = false,
        val error: String? = null,
    ) : VaultUiState()
    data class Viewing(
        val item: VaultItem,
        val items: List<VaultItem>,
        val image: ImageBitmap? = null,
        val playbackUri: String? = null,
        val busy: Boolean = false,
        val error: String? = null,
        val openedCollection: String? = null,
    ) : VaultUiState()
}

class VaultLockViewModel(
    private val repository: VaultRepository,
    private val biometric: BiometricUnlock,
) : ViewModel() {

    private val setupLock = SetupLockUseCase(repository)
    private val unlockVault = UnlockVaultUseCase(repository)
    private val changeLock = ChangeLockUseCase(repository)
    private val exportFromVault = ExportFromVaultUseCase(repository)
    private val deleteFromVault = DeleteFromVaultUseCase(repository)

    private val _state = MutableStateFlow<VaultUiState>(VaultUiState.Loading)
    val state: StateFlow<VaultUiState> = _state.asStateFlow()
    private var playbackTempPath: String? = null
    private var openGeneration: Int = 0
    private val thumbSlots = Semaphore(2)
    private val thumbCache = LinkedHashMap<String, ImageBitmap>(16, 0.75f, true)

    init {
        refresh()
    }

    fun refresh() {
        _state.value = when {
            !repository.isLockConfigured() -> VaultUiState.NeedsSetup
            !repository.isUnlocked() -> lockedState()
            else -> unlockedState()
        }
    }

    fun setupPin(pin: CharArray) {
        _state.value = VaultUiState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { setupLock.setupPin(pin) }
                _state.value = unlockedState(offerBiometric = biometric.isAvailable())
            } catch (_: Exception) {
                _state.value = VaultUiState.NeedsSetup
            } finally {
                pin.fill('\u0000')
            }
        }
    }

    fun setupPattern(cells: IntArray) {
        _state.value = VaultUiState.Loading
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { setupLock.setupPattern(cells) }
                _state.value = unlockedState(offerBiometric = biometric.isAvailable())
            } catch (_: Exception) {
                _state.value = VaultUiState.NeedsSetup
            }
        }
    }

    fun unlockWithPin(pin: CharArray) {
        val current = _state.value as? VaultUiState.Locked ?: return
        if (rejectIfLockedOut(current)) {
            pin.fill('\u0000')
            return
        }
        biometric.cancel()
        _state.value = current.copy(busy = true, error = null, pinFallback = true)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { unlockVault.unlockWithPin(pin) }
            pin.fill('\u0000')
            if (ok) {
                _state.value = unlockedState(offerBiometric = shouldOfferBiometric())
            } else {
                _state.value = lockedState(
                    error = failedUnlockMessage("PIN incorrecto"),
                    pinFallback = true,
                )
            }
        }
    }

    fun unlockWithPattern(cells: IntArray) {
        val current = _state.value as? VaultUiState.Locked ?: return
        if (rejectIfLockedOut(current)) return
        biometric.cancel()
        _state.value = current.copy(busy = true, error = null, pinFallback = true)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { unlockVault.unlockWithPattern(cells) }
            if (ok) {
                _state.value = unlockedState(offerBiometric = shouldOfferBiometric())
            } else {
                _state.value = lockedState(
                    error = failedUnlockMessage("Patrón incorrecto"),
                    pinFallback = true,
                )
            }
        }
    }

    fun unlockWithBiometric() {
        val current = _state.value as? VaultUiState.Locked ?: return
        if (rejectIfLockedOut(current)) return
        if (!biometric.isAvailable()) {
            _state.value = current.copy(
                busy = false,
                pinFallback = true,
                error = "No hay huella en este dispositivo",
            )
            return
        }
        biometric.cancel()
        _state.value = current.copy(busy = true, error = null)
        val wrapped = repository.biometricWrappedDek() ?: byteArrayOf()
        biometric.unwrap(wrapped) { result ->
            viewModelScope.launch {
                try {
                    if (_state.value !is VaultUiState.Locked) return@launch
                    val ok = result != null && withContext(Dispatchers.Default) {
                        if (result.size == DerivedKey.SIZE_BYTES) {
                            repository.unlockWithDek(result)
                        } else {
                            repository.unlockWithStoredDeviceKey()
                        }
                    }
                    if (ok) {
                        _state.value = unlockedState()
                    } else {
                        _state.value = lockedState(
                            error = "No se pudo usar la huella. Entra con el PIN.",
                            pinFallback = true,
                        )
                    }
                } catch (_: Exception) {
                    _state.value = lockedState(
                        error = "No se pudo usar la huella. Entra con el PIN.",
                        pinFallback = true,
                    )
                } finally {
                    result?.fill(0)
                }
            }
        }
    }

    fun cancelBiometricPrompt() {
        biometric.cancel()
        val current = _state.value as? VaultUiState.Locked ?: return
        if (current.busy) {
            _state.value = current.copy(busy = false)
        }
    }

    fun enableBiometric() {
        val dek = repository.copySessionDek() ?: return
        try {
            biometric.wrap(dek) { wrapped ->
                dek.fill(0)
                viewModelScope.launch {
                    try {
                        if (wrapped != null) {
                            repository.storeBiometricWrappedDek(wrapped)
                            _state.value = unlockedState(message = "Huella activada")
                        } else {
                            _state.value = unlockedState(message = "No se pudo activar la huella. El PIN sigue activo.")
                        }
                    } catch (_: Exception) {
                        _state.value = unlockedState(message = "No se pudo activar la huella. El PIN sigue activo.")
                    }
                }
            }
        } catch (_: Exception) {
            dek.fill(0)
            _state.value = unlockedState(message = "No se pudo activar la huella. El PIN sigue activo.")
        }
    }

    fun usePinFallback() {
        biometric.cancel()
        val current = _state.value as? VaultUiState.Locked ?: return
        _state.value = current.copy(pinFallback = true, error = null, busy = false)
    }

    fun skipBiometric() {
        val current = _state.value as? VaultUiState.Unlocked ?: return
        _state.value = current.copy(offerBiometric = false)
    }

    fun startChangeLock() {
        val current = _state.value as? VaultUiState.Unlocked ?: return
        val method = repository.configuredLockMethod() ?: return
        _state.value = VaultUiState.ChangingLock(
            currentMethod = method,
            items = current.items,
            openedCollection = current.openedCollection,
        )
    }

    fun cancelChangeLock() {
        val current = _state.value as? VaultUiState.ChangingLock ?: return
        _state.value = unlockedState(openedCollection = current.openedCollection)
    }

    fun changeLockPin(currentSecret: CharArray, pin: CharArray) {
        applyLockChange(currentSecret) {
            try {
                changeLock.changeToPin(currentSecret, pin)
            } finally {
                pin.fill('\u0000')
            }
        }
    }

    fun changeLockPattern(currentSecret: CharArray, cells: IntArray) {
        applyLockChange(currentSecret) { changeLock.changeToPattern(currentSecret, cells) }
    }

    private fun applyLockChange(currentSecret: CharArray, block: () -> Unit) {
        val current = _state.value as? VaultUiState.ChangingLock ?: return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { block() }
                _state.value = unlockedState(
                    message = "Candado actualizado",
                    openedCollection = current.openedCollection,
                )
            } catch (_: Exception) {
                _state.value = current.copy(busy = false, error = "El candado actual no coincide")
            } finally {
                currentSecret.fill('\u0000')
            }
        }
    }

    fun openItem(item: VaultItem, collection: List<VaultItem> = emptyList()) {
        val current = _state.value as? VaultUiState.Unlocked ?: return
        val pages = collection.ifEmpty { current.items }
        openFromList(item, pages, current.openedCollection)
    }

    fun openCollection(title: String) {
        val current = _state.value as? VaultUiState.Unlocked ?: return
        if (vaultCollectionByTitle(current.items, title) == null) return
        _state.value = current.copy(openedCollection = title)
    }

    fun closeCollection() {
        val current = _state.value as? VaultUiState.Unlocked ?: return
        _state.value = current.copy(openedCollection = null)
    }

    fun showItemAt(index: Int) {
        val current = _state.value as? VaultUiState.Viewing ?: return
        val item = current.items.getOrNull(index) ?: return
        if (item.id == current.item.id) return
        openFromList(item, current.items, current.openedCollection)
    }

    suspend fun loadThumbnail(item: VaultItem): ImageBitmap? = withContext(Dispatchers.Default) {
        cachedThumb(item.id.value)?.let { return@withContext it }
        thumbSlots.withPermit {
            cachedThumb(item.id.value)?.let { return@withPermit it }
            val jpeg = try {
                repository.readOrCreateThumbnail(item)
            } catch (_: Exception) {
                null
            } ?: return@withPermit null
            try {
                val image = try {
                    decodeVaultImage(jpeg)
                } catch (_: Exception) {
                    null
                }
                if (image != null) storeThumb(item.id.value, image)
                image
            } finally {
                jpeg.fill(0)
            }
        }
    }

    private fun openFromList(
        item: VaultItem,
        items: List<VaultItem>,
        openedCollection: String?,
    ) {
        val generation = openGeneration + 1
        openGeneration = generation
        _state.value = VaultUiState.Viewing(
            item = item,
            items = items,
            busy = item.mediaKind == VaultMediaKind.VIDEO,
            openedCollection = openedCollection,
        )
        viewModelScope.launch {
            var playbackUri: String? = null
            try {
                val image = withContext(Dispatchers.Default) {
                    if (item.mediaKind == VaultMediaKind.IMAGE) {
                        val bytes = runCatching { repository.readDecrypted(item.id) }.getOrNull()
                        try {
                            bytes?.let { runCatching { decodeVaultImage(it) }.getOrNull() }
                                ?: loadThumbnail(item)
                        } finally {
                            bytes?.fill(0)
                        }
                    } else {
                        loadThumbnail(item)
                    }
                }
                if (generation != openGeneration) return@launch
                playbackUri = if (item.mediaKind == VaultMediaKind.VIDEO) {
                    withContext(Dispatchers.IO) {
                        runCatching { repository.prepareVideoPlayback(item) }.getOrNull()
                    }
                } else {
                    null
                }
                if (generation != openGeneration) {
                    playbackUri?.let { path ->
                        withContext(Dispatchers.IO) { runCatching { deleteTempPlayableVideo(path) } }
                    }
                    return@launch
                }
                replacePlaybackTemp(playbackUri)
                _state.value = VaultUiState.Viewing(
                    item = item,
                    items = items,
                    image = image,
                    playbackUri = playbackUri,
                    openedCollection = openedCollection,
                    error = if (item.mediaKind == VaultMediaKind.VIDEO && playbackUri == null) {
                        "No se pudo reproducir el video"
                    } else {
                        null
                    },
                )
            } catch (_: Throwable) {
                if (generation != openGeneration) {
                    playbackUri?.let { path ->
                        withContext(Dispatchers.IO) { runCatching { deleteTempPlayableVideo(path) } }
                    }
                    return@launch
                }
                replacePlaybackTemp(null)
                _state.value = VaultUiState.Viewing(
                    item = item,
                    items = items,
                    openedCollection = openedCollection,
                    error = "No se pudo abrir",
                )
            }
        }
    }

    fun closeViewer() {
        val current = _state.value as? VaultUiState.Viewing ?: return
        openGeneration += 1
        replacePlaybackTemp(null)
        _state.value = unlockedState(openedCollection = current.openedCollection)
    }

    fun exportCurrent() {
        val current = _state.value as? VaultUiState.Viewing ?: return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { exportFromVault(current.item.id) }
                replacePlaybackTemp(null)
                _state.value = unlockedState(
                    message = "Exportado a la galería pública",
                    openedCollection = current.openedCollection,
                )
            } catch (_: Exception) {
                _state.value = current.copy(busy = false, error = "Error al exportar")
            }
        }
    }

    fun deleteCurrent() {
        val current = _state.value as? VaultUiState.Viewing ?: return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { deleteFromVault(current.item.id) }
                replacePlaybackTemp(null)
                synchronized(thumbCache) { thumbCache.remove(current.item.id.value) }
                _state.value = unlockedState(
                    message = "Borrado del disco. No se puede recuperar.",
                    openedCollection = current.openedCollection,
                )
            } catch (_: Exception) {
                _state.value = current.copy(busy = false, error = "Error al borrar")
            }
        }
    }

    fun lockVault() {
        try {
            biometric.cancel()
            openGeneration += 1
            replacePlaybackTemp(null)
            clearThumbs()
            if (repository.resetLockIfEmpty()) {
                _state.value = VaultUiState.NeedsSetup
            } else {
                repository.lock()
                _state.value = if (repository.isLockConfigured()) {
                    lockedState()
                } else {
                    VaultUiState.NeedsSetup
                }
            }
        } catch (_: Exception) {
            runCatching { repository.lock() }
            _state.value = if (repository.isLockConfigured()) {
                lockedState()
            } else {
                VaultUiState.NeedsSetup
            }
        }
    }

    override fun onCleared() {
        runCatching { lockVault() }
        super.onCleared()
    }

    private fun replacePlaybackTemp(path: String?) {
        val previous = playbackTempPath
        playbackTempPath = path
        if (previous != null && previous != path) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { deleteTempPlayableVideo(previous) }
            }
        }
    }

    private fun shouldOfferBiometric(): Boolean =
        biometric.isAvailable() && !repository.isBiometricEnabled()

    private fun rejectIfLockedOut(current: VaultUiState.Locked): Boolean {
        val remaining = repository.lockoutRemainingMs()
        if (remaining <= 0L) return false
        biometric.cancel()
        _state.value = current.copy(
            busy = false,
            error = lockoutMessage(remaining),
            lockoutRemainingMs = remaining,
        )
        return true
    }

    private fun failedUnlockMessage(fallback: String): String {
        val remaining = repository.lockoutRemainingMs()
        return if (remaining > 0L) lockoutMessage(remaining) else fallback
    }

    private fun lockedState(
        error: String? = null,
        pinFallback: Boolean? = null,
    ): VaultUiState.Locked {
        val available = biometric.isAvailable()
        val remaining = repository.lockoutRemainingMs()
        val canUseFingerprint = remaining <= 0L &&
            available &&
            (repository.isBiometricEnabled() || repository.canHideWithoutUnlock())
        return VaultUiState.Locked(
            method = repository.configuredLockMethod() ?: LockMethod.PIN,
            biometricAvailable = available,
            biometricEnabled = canUseFingerprint,
            pinFallback = pinFallback ?: false,
            error = error,
            lockoutRemainingMs = remaining,
        )
    }

    private fun unlockedState(
        offerBiometric: Boolean = false,
        message: String? = null,
        openedCollection: String? = null,
    ): VaultUiState {
        return try {
            val items = repository.listItems()
            val keptCollection = openedCollection
                ?.takeIf { vaultCollectionByTitle(items, it) != null }
            VaultUiState.Unlocked(
                items = items,
                offerBiometric = offerBiometric,
                message = message,
                openedCollection = keptCollection,
            )
        } catch (_: Exception) {
            repository.lock()
            lockedState(error = "No se pudo abrir el Vault")
        }
    }

    private fun cachedThumb(id: String): ImageBitmap? = synchronized(thumbCache) { thumbCache[id] }

    private fun storeThumb(id: String, image: ImageBitmap) {
        synchronized(thumbCache) {
            if (thumbCache.size >= 24) {
                val oldest = thumbCache.keys.firstOrNull()
                if (oldest != null) thumbCache.remove(oldest)
            }
            thumbCache[id] = image
        }
    }

    private fun clearThumbs() {
        synchronized(thumbCache) { thumbCache.clear() }
    }
}

internal fun lockoutMessage(remainingMs: Long): String {
    val seconds = ((remainingMs + 999) / 1000).coerceAtLeast(1L)
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes <= 0L -> "Demasiados intentos. Espera ${seconds}s."
        rest == 0L -> "Demasiados intentos. Espera $minutes min."
        else -> "Demasiados intentos. Espera $minutes min ${rest}s."
    }
}
