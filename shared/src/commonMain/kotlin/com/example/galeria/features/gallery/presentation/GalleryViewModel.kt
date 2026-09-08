package com.example.galeria.features.gallery.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.galeria.core.HideInVault
import com.example.galeria.features.editor.domain.EditImageUseCase
import com.example.galeria.features.editor.domain.ImageEdit
import com.example.galeria.features.editor.presentation.decodeImageBytes
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.MediaLibraryWatcher
import com.example.galeria.features.gallery.domain.MediaRepository
import com.example.galeria.features.gallery.data.CollectionStore
import com.example.galeria.features.gallery.domain.ScanMediaUseCase
import com.example.galeria.features.gallery.domain.UserCollection
import com.example.galeria.features.gallery.domain.movingTo
import com.example.galeria.features.gallery.domain.newUserCollection
import com.example.galeria.features.share.domain.ShareMediaUseCase
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GalleryUiState {
    data object NeedsPermission : GalleryUiState()
    data object Loading : GalleryUiState()
    data class Ready(
        val albums: List<Album>,
        val allItems: List<MediaItem>,
        val favorites: Set<String> = emptySet(),
        val selectedIds: Set<String> = emptySet(),
        val openedAlbum: Album? = null,
        val viewing: ViewerSession? = null,
        val editing: EditorSession? = null,
        val userCollections: List<UserCollection> = emptyList(),
        val message: String? = null,
        val busy: Boolean = false,
        val error: String? = null,
    ) : GalleryUiState() {
        val selecting: Boolean get() = selectedIds.isNotEmpty()
    }
}

data class ViewerSession(
    val items: List<MediaItem>,
    val index: Int,
    val image: ImageBitmap? = null,
    val busy: Boolean = false,
    val error: String? = null,
) {
    val item: MediaItem get() = items[index]
}

data class EditorSession(
    val item: MediaItem,
    val viewing: ViewerSession,
    val preview: ImageBitmap? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

class GalleryViewModel(
    private val scanMedia: ScanMediaUseCase,
    private val mediaRepository: MediaRepository,
    private val editImage: EditImageUseCase,
    private val hideInVault: HideInVault,
    private val permission: MediaPermission,
    private val shareMedia: ShareMediaUseCase,
    private val collectionStore: CollectionStore,
    private val libraryWatcher: MediaLibraryWatcher,
) : ViewModel() {

    private val _state = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    private val _unlockForHide = MutableStateFlow(false)
    val unlockForHide: StateFlow<Boolean> = _unlockForHide.asStateFlow()

    private val _lockVaultAfterHide = MutableStateFlow(false)
    val lockVaultAfterHide: StateFlow<Boolean> = _lockVaultAfterHide.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var pendingHide: List<MediaItem> = emptyList()
    private var editorSource: ByteArray? = null
    private var previewJob: Job? = null
    private var debounceJob: Job? = null
    private var scanJob: Job? = null
    private var favorites: Set<String> = emptySet()
    private var userCollections: List<UserCollection> = emptyList()
    private var watchingLibrary = false
    private var scanningLibrary = false
    private var suppressLibraryEvents = false
    private var lastFingerprint = 0L
    private var burstStartedAt: kotlin.time.TimeMark? = null
    private var pendingUserRefresh = false

    init {
        userCollections = collectionStore.load()
        refresh()
    }

    fun refresh() {
        refreshLibrary(userInitiated = true)
    }

    private fun onLibraryChanged() {
        if (suppressLibraryEvents || scanningLibrary) return
        val now = TimeSource.Monotonic.markNow()
        val start = burstStartedAt ?: now.also { burstStartedAt = it }
        val wait = libraryRefreshDelayMs(start.elapsedNow().inWholeMilliseconds)
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(wait)
            burstStartedAt = null
            if (!suppressLibraryEvents) {
                refreshLibrary(userInitiated = false)
            }
        }
    }

    private fun refreshLibrary(userInitiated: Boolean) {
        if (!permission.hasAccess()) {
            stopWatching()
            _state.value = GalleryUiState.NeedsPermission
            _refreshing.value = false
            return
        }
        ensureWatching()
        if (scanningLibrary) {
            if (userInitiated) pendingUserRefresh = true
            return
        }
        val previous = _state.value
        if (userInitiated) {
            if (previous !is GalleryUiState.Ready) {
                _state.value = GalleryUiState.Loading
            } else {
                _refreshing.value = true
            }
        }
        scanningLibrary = true
        suppressLibraryEvents = true
        scanJob = viewModelScope.launch {
            try {
                val albums = withContext(Dispatchers.Default) {
                    runCatching { scanMedia() }.getOrNull()
                }
                if (albums == null) {
                    if (previous !is GalleryUiState.Ready) {
                        _state.value = GalleryUiState.Ready(
                            albums = emptyList(),
                            allItems = emptyList(),
                            userCollections = userCollections,
                            error = "Error al leer la galería",
                        )
                    }
                    return@launch
                }
                val allItems = albums.flatMap { it.items }.distinctBy { it.id }
                    .sortedByDescending { it.capturedAtEpochMs }
                val fingerprint = libraryFingerprint(allItems)
                if (previous is GalleryUiState.Ready && fingerprint == lastFingerprint) {
                    return@launch
                }
                lastFingerprint = fingerprint
                _state.value = toReady(previous, albums, allItems)
            } finally {
                _refreshing.value = false
                scanningLibrary = false
                suppressLibraryEvents = false
                val againUser = pendingUserRefresh
                pendingUserRefresh = false
                if (againUser) {
                    refreshLibrary(userInitiated = true)
                }
            }
        }
    }

    private fun ensureWatching() {
        if (watchingLibrary) return
        watchingLibrary = true
        libraryWatcher.start(::onLibraryChanged)
    }

    private fun stopWatching() {
        if (!watchingLibrary) return
        watchingLibrary = false
        debounceJob?.cancel()
        burstStartedAt = null
        libraryWatcher.stop()
    }

    fun requestPermission() {
        permission.request { granted ->
            viewModelScope.launch {
                if (granted) {
                    refresh()
                } else {
                    stopWatching()
                    _state.value = GalleryUiState.NeedsPermission
                }
            }
        }
    }

    fun openAlbum(album: Album) {
        updateReady { it.copy(openedAlbum = album, message = null) }
    }

    fun closeAlbum() {
        updateReady { it.copy(openedAlbum = null, selectedIds = emptySet()) }
    }

    fun createAlbum(
        title: String,
        itemIds: List<String> = emptyList(),
        sourceCustomId: String? = null,
    ) {
        val name = title.trim()
        if (name.isEmpty()) return
        val collection = newUserCollection(userCollections, name, itemIds)
        userCollections = (userCollections + collection).movingTo(collection.id, itemIds, sourceCustomId)
        collectionStore.save(userCollections)
        updateReady { current ->
            val albums = mergeCustomAlbums(scannedAlbums(current.albums), current.allItems)
            current.copy(
                albums = albums,
                userCollections = userCollections,
                openedAlbum = albums.firstOrNull { it.customId == collection.id },
                selectedIds = emptySet(),
                viewing = null,
                message = if (itemIds.isEmpty()) "Álbum creado" else "Movido al álbum",
            )
        }
    }

    fun moveToAlbum(
        collectionId: String,
        itemIds: List<String>,
        sourceCustomId: String? = null,
    ) {
        if (itemIds.isEmpty()) return
        userCollections = userCollections.movingTo(collectionId, itemIds, sourceCustomId)
        collectionStore.save(userCollections)
        updateReady { current ->
            val albums = mergeCustomAlbums(scannedAlbums(current.albums), current.allItems)
            current.copy(
                albums = albums,
                userCollections = userCollections,
                openedAlbum = albums.firstOrNull { it.customId == collectionId } ?: current.openedAlbum,
                selectedIds = emptySet(),
                viewing = null,
                message = "Movido al álbum",
            )
        }
    }

    fun openItem(item: MediaItem, collection: List<MediaItem>) {
        val ready = ready() ?: return
        if (ready.selecting) {
            toggleSelect(item)
            return
        }
        val items = collection.ifEmpty { ready.allItems }
        val index = items.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        viewModelScope.launch {
            val image = decodeItem(items[index])
            updateReady {
                it.copy(
                    viewing = ViewerSession(items = items, index = index, image = image),
                    error = null,
                )
            }
        }
    }

    fun showViewerIndex(index: Int) {
        val ready = ready() ?: return
        val viewing = ready.viewing ?: return
        if (index !in viewing.items.indices) return
        if (index == viewing.index && viewing.image != null) return
        viewModelScope.launch {
            val image = decodeItem(viewing.items[index])
            updateReady { current ->
                val session = current.viewing ?: return@updateReady current
                current.copy(viewing = session.copy(index = index, image = image, error = null))
            }
        }
    }

    fun closeViewer() {
        updateReady { it.copy(viewing = null, error = null) }
    }

    fun shareCurrent() {
        val viewing = ready()?.viewing ?: return
        shareItems(listOf(viewing.item))
    }

    fun shareSelected() {
        val ready = ready() ?: return
        shareItems(selectedMedia(ready))
    }

    fun shareItems(items: List<MediaItem>) {
        val first = items.firstOrNull() ?: return
        val result = runCatching { shareMedia(first.publicUri, mimeType(first)) }
        if (result.isFailure) {
            updateReady { current ->
                current.copy(
                    viewing = current.viewing?.copy(error = "Error al compartir"),
                    error = "Error al compartir",
                )
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        favorites = if (item.id in favorites) favorites - item.id else favorites + item.id
        updateReady { it.copy(favorites = favorites) }
    }

    fun startSelection(item: MediaItem) {
        updateReady { it.copy(selectedIds = it.selectedIds + item.id) }
    }

    fun toggleSelect(item: MediaItem) {
        updateReady {
            val next = if (item.id in it.selectedIds) it.selectedIds - item.id else it.selectedIds + item.id
            it.copy(selectedIds = next)
        }
    }

    fun clearSelection() {
        updateReady { it.copy(selectedIds = emptySet()) }
    }

    fun editItem(item: MediaItem) {
        if (item.mediaKind != GalleryMediaKind.IMAGE) return
        viewModelScope.launch {
            val image = decodeItem(item)
            val viewing = ViewerSession(items = listOf(item), index = 0, image = image)
            val bytes = withContext(Dispatchers.Default) {
                runCatching { loadEditJpeg(item.publicUri, 4096) }.getOrNull()
                    ?: runCatching { mediaRepository.readBytes(item.publicUri) }.getOrNull()
            }
            if (bytes == null) {
                updateReady { it.copy(viewing = viewing.copy(error = "Error al abrir el editor")) }
                return@launch
            }
            editorSource = bytes
            updateReady {
                it.copy(
                    viewing = viewing,
                    editing = EditorSession(item = item, viewing = viewing, preview = image),
                )
            }
        }
    }

    fun openEditor() {
        val ready = ready() ?: return
        val viewing = ready.viewing ?: return
        if (viewing.item.mediaKind != GalleryMediaKind.IMAGE) return
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                runCatching { loadEditJpeg(viewing.item.publicUri, 4096) }.getOrNull()
                    ?: runCatching { mediaRepository.readBytes(viewing.item.publicUri) }.getOrNull()
            }
            if (bytes == null) {
                updateReady {
                    it.copy(viewing = it.viewing?.copy(error = "Error al abrir el editor"))
                }
                return@launch
            }
            editorSource = bytes
            updateReady {
                it.copy(
                    editing = EditorSession(
                        item = viewing.item,
                        viewing = viewing,
                        preview = viewing.image,
                    ),
                )
            }
        }
    }

    fun previewEdit(edit: ImageEdit) {
        val current = ready()?.editing ?: return
        val source = editorSource ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(120)
            val previewBytes = withContext(Dispatchers.Default) {
                runCatching { editImage(source, edit, PREVIEW_EDGE) }.getOrNull()
            } ?: return@launch
            val image = withContext(Dispatchers.Default) { decodeImageBytes(previewBytes) }
            previewBytes.fill(0)
            updateReady { latest ->
                val editing = latest.editing ?: return@updateReady latest
                latest.copy(editing = editing.copy(preview = image))
            }
        }
    }

    fun saveEdit(edit: ImageEdit) {
        val current = ready()?.editing ?: return
        val source = editorSource ?: return
        updateReady { it.copy(editing = current.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val jpeg = editImage(source, edit, 0)
                    val name = editedName(current.item.displayName)
                    mediaRepository.saveJpeg(name, jpeg)
                    jpeg.fill(0)
                }
            }
            if (result.isFailure) {
                updateReady {
                    it.copy(editing = it.editing?.copy(busy = false, error = "Error al guardar la edición"))
                }
                return@launch
            }
            wipeEditorSource()
            val albums = withContext(Dispatchers.Default) {
                runCatching { scanMedia() }.getOrDefault(emptyList())
            }
            _state.value = applyScanned(_state.value, albums).copy(
                editing = null,
                viewing = null,
                message = "Edición guardada",
            )
        }
    }

    fun closeEditor() {
        val editing = ready()?.editing ?: return
        wipeEditorSource()
        updateReady { it.copy(editing = null, viewing = editing.viewing.copy(image = editing.preview)) }
    }

    fun moveCurrentToVault() {
        val item = ready()?.viewing?.item ?: return
        moveToVault(listOf(item))
    }

    fun moveSelectedToVault() {
        val ready = ready() ?: return
        moveToVault(selectedMedia(ready))
    }

    fun moveToVault(items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (hideInVault.isReady()) {
            hideItems(items, lockWhenDone = true)
            return
        }
        pendingHide = items
        _unlockForHide.value = true
    }

    fun cancelHideUnlock() {
        pendingHide = emptyList()
        _unlockForHide.value = false
    }

    fun consumeLockVaultAfterHide() {
        _lockVaultAfterHide.value = false
    }

    fun onVaultSessionReady() {
        val items = pendingHide
        if (items.isEmpty()) return
        if (!hideInVault.isReady()) return
        pendingHide = emptyList()
        hideItems(items, lockWhenDone = true)
    }

    fun deleteCurrent() {
        val item = ready()?.viewing?.item ?: return
        deleteItems(listOf(item))
    }

    fun deleteSelected() {
        val ready = ready() ?: return
        deleteItems(selectedMedia(ready))
    }

    private fun deleteItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        updateReady { it.copy(busy = true, viewing = it.viewing?.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { mediaRepository.deleteAll(items.map { it.publicUri }) }
            }
            if (result.isFailure) {
                val denied = result.exceptionOrNull()?.message.orEmpty()
                    .contains("denied", ignoreCase = true)
                val message = if (denied) {
                    "Android pide confirmación para borrar. Pulsa Permitir en el aviso."
                } else {
                    "Error al eliminar. Confirma el aviso del sistema e inténtalo de nuevo."
                }
                updateReady {
                    it.copy(
                        busy = false,
                        viewing = it.viewing?.copy(busy = false, error = message),
                        error = message,
                        message = message,
                    )
                }
                return@launch
            }
            val albums = withContext(Dispatchers.Default) {
                runCatching { scanMedia() }.getOrDefault(emptyList())
            }
            val message = if (items.size == 1) "Eliminado" else "Eliminados ${items.size} elementos"
            _state.value = applyScanned(_state.value, albums).copy(
                viewing = null,
                selectedIds = emptySet(),
                message = message,
            )
        }
    }

    private fun hideItems(items: List<MediaItem>, lockWhenDone: Boolean) {
        val previousViewing = ready()?.viewing
        updateReady { it.copy(busy = true, viewing = it.viewing?.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching { hideInVault.hideAll(items.map { it.toPublicMediaRef() }) }
            }
            val albums = withContext(Dispatchers.Default) {
                runCatching { scanMedia() }.getOrDefault(emptyList())
            }
            val denied = result.exceptionOrNull()?.message.orEmpty().contains("denied", ignoreCase = true)
            val message = when {
                result.isSuccess && items.size == 1 -> "Movido al Vault"
                result.isSuccess -> "Movidos ${items.size} al Vault"
                denied -> "Hay que permitir borrar el original"
                else -> "Error al mover al Vault"
            }
            val movedIds = items.map { it.id }.toSet()
            val nextViewing = if (result.isSuccess) {
                viewerAfterHide(previousViewing, movedIds)
            } else {
                previousViewing?.copy(busy = false, error = message)
            }
            val image = nextViewing?.let { session ->
                decodeItem(session.items[session.index])
            }
            _unlockForHide.value = false
            if (lockWhenDone) {
                _lockVaultAfterHide.value = true
            }
            _state.value = applyScanned(_state.value, albums).copy(
                viewing = nextViewing?.copy(image = image, busy = false),
                selectedIds = emptySet(),
                message = if (result.isSuccess) message else null,
                error = if (result.isFailure && nextViewing == null) message else null,
                busy = false,
            )
        }
    }

    fun dismissMessage() {
        updateReady { it.copy(message = null, error = null) }
    }

    private fun selectedMedia(ready: GalleryUiState.Ready): List<MediaItem> =
        ready.allItems.filter { it.id in ready.selectedIds }

    private suspend fun decodeItem(item: MediaItem): ImageBitmap? {
        val edge = if (item.mediaKind == GalleryMediaKind.VIDEO) 1280 else 1920
        return withContext(Dispatchers.Default) { decodePublicImage(item.publicUri, edge) }
    }

    private fun ready(): GalleryUiState.Ready? = _state.value as? GalleryUiState.Ready

    private fun updateReady(transform: (GalleryUiState.Ready) -> GalleryUiState.Ready) {
        val current = _state.value as? GalleryUiState.Ready ?: return
        _state.value = transform(current)
    }

    private fun applyScanned(previous: GalleryUiState, albums: List<Album>): GalleryUiState.Ready {
        val allItems = albums.flatMap { it.items }.distinctBy { it.id }
            .sortedByDescending { it.capturedAtEpochMs }
        lastFingerprint = libraryFingerprint(allItems)
        return toReady(previous, albums, allItems)
    }

    private fun toReady(
        previous: GalleryUiState,
        albums: List<Album>,
        allItems: List<MediaItem>,
    ): GalleryUiState.Ready {
        val prevReady = previous as? GalleryUiState.Ready
        val merged = mergeCustomAlbums(albums, allItems)
        val opened = restoreOpenedAlbum(prevReady?.openedAlbum, merged, allItems)
        val available = allItems.associateBy { it.id }
        val selected = prevReady?.selectedIds.orEmpty().filter { it in available }.toSet()
        val editing = prevReady?.editing?.takeIf { it.item.id in available }
        return GalleryUiState.Ready(
            albums = merged,
            allItems = allItems,
            favorites = favorites,
            selectedIds = selected,
            openedAlbum = opened,
            viewing = viewingAfterLibraryChange(prevReady?.viewing, available),
            editing = editing,
            userCollections = userCollections,
            message = prevReady?.message,
        )
    }

    private fun scannedAlbums(albums: List<Album>): List<Album> =
        albums.filter { it.customId == null }

    private fun mergeCustomAlbums(scanned: List<Album>, allItems: List<MediaItem>): List<Album> {
        val byId = allItems.associateBy { it.id }
        val custom = userCollections.map { collection ->
            Album(
                id = AlbumId.OTHER,
                title = collection.title,
                items = collection.itemIds.mapNotNull { byId[it] },
                customId = collection.id,
            )
        }
        return custom + scanned
    }

    private fun restoreOpenedAlbum(
        previous: Album?,
        albums: List<Album>,
        allItems: List<MediaItem>,
    ): Album? {
        if (previous == null) return null
        if (previous.customId != null) {
            val collection = userCollections.firstOrNull { it.id == previous.customId } ?: return null
            val byId = allItems.associateBy { it.id }
            return Album(
                id = AlbumId.OTHER,
                title = collection.title,
                items = collection.itemIds.mapNotNull { byId[it] },
                customId = collection.id,
            )
        }
        if (previous.title == "Favoritos") {
            val favItems = allItems.filter { it.id in favorites }
            return if (favItems.isEmpty()) null else Album(AlbumId.OTHER, "Favoritos", favItems)
        }
        if (previous.title == VIDEOS_COLLECTION_TITLE) {
            val videos = allItems.filter { it.mediaKind == GalleryMediaKind.VIDEO }
            return if (videos.isEmpty()) null else Album(AlbumId.OTHER, VIDEOS_COLLECTION_TITLE, videos)
        }
        return albums.firstOrNull { it.id == previous.id && it.title == previous.title }
            ?: albums.firstOrNull { it.id == previous.id }
    }

    override fun onCleared() {
        stopWatching()
        scanJob?.cancel()
        wipeEditorSource()
        super.onCleared()
    }

    private fun wipeEditorSource() {
        previewJob?.cancel()
        editorSource?.fill(0)
        editorSource = null
    }
}

internal fun viewerAfterHide(session: ViewerSession?, movedIds: Set<String>): ViewerSession? {
    if (session == null) return null
    val remaining = session.items.filter { it.id !in movedIds }
    if (remaining.isEmpty()) return null
    val nextIndex = session.index.coerceAtMost(remaining.lastIndex).coerceAtLeast(0)
    return ViewerSession(items = remaining, index = nextIndex)
}

private const val PREVIEW_EDGE = 1280

internal fun mimeType(item: MediaItem): String {
    val name = item.displayName.lowercase()
    return when (item.mediaKind) {
        GalleryMediaKind.IMAGE -> when {
            name.endsWith(".png") -> "image/png"
            name.endsWith(".webp") -> "image/webp"
            name.endsWith(".gif") -> "image/gif"
            name.endsWith(".heic") -> "image/heic"
            else -> "image/jpeg"
        }
        GalleryMediaKind.VIDEO -> when {
            name.endsWith(".mov") -> "video/quicktime"
            name.endsWith(".webm") -> "video/webm"
            else -> "video/mp4"
        }
    }
}

private fun editedName(original: String): String {
    val dot = original.lastIndexOf('.')
    val base = if (dot > 0) original.substring(0, dot) else original
    return "${base}_edit.jpg"
}
