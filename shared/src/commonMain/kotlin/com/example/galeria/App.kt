package com.example.galeria

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galeria.features.editor.domain.EditImageUseCase
import com.example.galeria.features.editor.domain.ImageEditor
import com.example.galeria.features.editor.presentation.EditorScreen
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaLibraryWatcher
import com.example.galeria.features.gallery.domain.ScanMediaUseCase
import com.example.galeria.features.gallery.domain.createMediaRepository
import com.example.galeria.features.gallery.data.CollectionStore
import com.example.galeria.features.gallery.data.FavoritesStore
import com.example.galeria.features.gallery.presentation.AlbumGridScreen
import com.example.galeria.features.gallery.presentation.AlbumMediaPickerScreen
import com.example.galeria.features.gallery.presentation.AlbumDeleteConfirmDialog
import com.example.galeria.features.gallery.presentation.AlbumNameDialog
import com.example.galeria.features.gallery.presentation.AlbumPickerDialog
import com.example.galeria.features.gallery.presentation.CollectionsScreen
import com.example.galeria.features.gallery.presentation.CreateScreen
import com.example.galeria.features.gallery.presentation.GalleryLoadingPane
import com.example.galeria.features.gallery.presentation.GalleryPermissionPane
import com.example.galeria.features.gallery.presentation.GalleryUiState
import com.example.galeria.features.gallery.presentation.GalleryViewModel
import com.example.galeria.features.gallery.presentation.GalleryViewerScreen
import com.example.galeria.features.gallery.presentation.ALL_VIDEOS_KEY
import com.example.galeria.features.gallery.presentation.classifyVideoAlbums
import com.example.galeria.features.gallery.presentation.videoAlbumKey
import com.example.galeria.features.gallery.presentation.PhotosScreen
import com.example.galeria.features.gallery.presentation.SearchScreen
import com.example.galeria.features.gallery.presentation.VIDEOS_COLLECTION_TITLE
import com.example.galeria.features.gallery.presentation.VideosScreen
import com.example.galeria.features.gallery.presentation.SelectionBar
import com.example.galeria.features.share.domain.ShareMediaUseCase
import com.example.galeria.features.share.presentation.ShareSheet
import com.example.galeria.features.vault.data.VaultComponent
import com.example.galeria.features.vault.data.createHideInVault
import com.example.galeria.features.vault.presentation.BiometricUnlock
import com.example.galeria.features.vault.presentation.VaultChangeLockScreen
import com.example.galeria.features.vault.presentation.VaultGridScreen
import com.example.galeria.features.vault.presentation.VaultLockViewModel
import com.example.galeria.features.vault.presentation.VaultSetupScreen
import com.example.galeria.features.vault.presentation.VaultUiState
import com.example.galeria.features.vault.presentation.VaultUnlockScreen
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.presentation.VaultViewerScreen
import com.example.galeria.ui.AccentsLavender
import com.example.galeria.ui.AppBackHandler
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.GaleriaTheme
import com.example.galeria.ui.setSensitiveWindow

private enum class AppTab { PHOTOS, VIDEOS, COLLECTIONS, CREATE }

@Composable
fun App() {
    GaleriaTheme {
        val galleryViewModel: GalleryViewModel = viewModel {
            val mediaRepository = createMediaRepository()
            GalleryViewModel(
                scanMedia = ScanMediaUseCase(mediaRepository),
                mediaRepository = mediaRepository,
                editImage = EditImageUseCase(ImageEditor()),
                hideInVault = createHideInVault(),
                permission = com.example.galeria.features.gallery.presentation.MediaPermission(),
                shareMedia = ShareMediaUseCase(ShareSheet()),
                collectionStore = CollectionStore(),
                favoritesStore = FavoritesStore(),
                libraryWatcher = MediaLibraryWatcher(),
            )
        }
        val vaultViewModel: VaultLockViewModel = viewModel {
            VaultLockViewModel(
                repository = VaultComponent.repository,
                biometric = BiometricUnlock(),
            )
        }
        val galleryState by galleryViewModel.state.collectAsState()
        val vaultState by vaultViewModel.state.collectAsState()
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    vaultViewModel.lockOnBackground()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val unlockForHide by galleryViewModel.unlockForHide.collectAsState()
        val lockVaultAfterHide by galleryViewModel.lockVaultAfterHide.collectAsState()
        val refreshing by galleryViewModel.refreshing.collectAsState()
        var tabName by rememberSaveable { mutableStateOf(AppTab.PHOTOS.name) }
        val tab = AppTab.entries.find { it.name == tabName } ?: AppTab.PHOTOS
        var videoAlbumKey by rememberSaveable { mutableStateOf(ALL_VIDEOS_KEY) }
        var searchOpen by remember { mutableStateOf(false) }
        fun selectTab(next: AppTab) {
            galleryViewModel.closeAlbum()
            if (next == AppTab.VIDEOS) videoAlbumKey = ALL_VIDEOS_KEY
            if (next == tab) return
            tabName = next.name
        }
        var vaultOpen by rememberSaveable { mutableStateOf(false) }
        val requestVault = { vaultOpen = true }
        var albumNameOpen by remember { mutableStateOf(false) }
        var albumPickerOpen by remember { mutableStateOf(false) }
        var pickMediaOpen by remember { mutableStateOf(false) }
        var pendingAlbumIds by remember { mutableStateOf<List<String>>(emptyList()) }
        var pendingAlbumName by remember { mutableStateOf<String?>(null) }
        var pendingTargetId by remember { mutableStateOf<String?>(null) }
        var fromCustomAlbumId by remember { mutableStateOf<String?>(null) }
        var renameAlbumOpen by remember { mutableStateOf(false) }
        var deleteAlbumOpen by remember { mutableStateOf(false) }
        fun currentReady() = galleryViewModel.state.value as? GalleryUiState.Ready
        fun customAlbums() = currentReady()?.albums.orEmpty().filter { it.customId != null }
        fun resetAlbumFlow() {
            albumNameOpen = false
            albumPickerOpen = false
            pickMediaOpen = false
            pendingAlbumIds = emptyList()
            pendingAlbumName = null
            pendingTargetId = null
            fromCustomAlbumId = null
        }
        fun requestCreateAlbum(itemIds: List<String> = pendingAlbumIds) {
            pendingAlbumIds = itemIds
            pendingAlbumName = null
            pendingTargetId = null
            albumPickerOpen = false
            pickMediaOpen = false
            albumNameOpen = true
        }
        fun requestMoveToAlbum(itemIds: List<String>, sourceCustomId: String? = null) {
            pendingAlbumIds = itemIds
            fromCustomAlbumId = sourceCustomId
            pendingAlbumName = null
            pendingTargetId = null
            pickMediaOpen = false
            val destinations = customAlbums().filter { it.customId != sourceCustomId }
            if (destinations.isEmpty()) {
                albumPickerOpen = false
                albumNameOpen = true
            } else {
                albumNameOpen = false
                albumPickerOpen = true
            }
        }
        fun requestPickMediaForAlbum(collectionId: String) {
            pendingTargetId = collectionId
            pendingAlbumName = null
            pendingAlbumIds = emptyList()
            fromCustomAlbumId = null
            albumNameOpen = false
            albumPickerOpen = false
            pickMediaOpen = true
        }

        LaunchedEffect(vaultState) {
            if (vaultState is VaultUiState.Unlocked) {
                galleryViewModel.onVaultSessionReady()
            }
        }
        LaunchedEffect(lockVaultAfterHide) {
            if (lockVaultAfterHide) {
                vaultOpen = false
                vaultViewModel.lockVault()
                galleryViewModel.consumeLockVaultAfterHide()
            }
        }
        LaunchedEffect(vaultOpen, vaultState) {
            val viewing = vaultState as? VaultUiState.Viewing
            val showingPrivate =
                vaultOpen &&
                    (vaultState is VaultUiState.Unlocked ||
                        viewing != null ||
                        vaultState is VaultUiState.Editing)
            val playingVaultVideo = viewing != null &&
                viewing.item.mediaKind == VaultMediaKind.VIDEO &&
                viewing.playbackUri != null
            setSensitiveWindow(showingPrivate && !playingVaultVideo)
        }

        val ready = galleryState as? GalleryUiState.Ready
        val overlay = ready?.editing != null ||
            ready?.viewing != null ||
            vaultState is VaultUiState.Viewing ||
            vaultState is VaultUiState.ChangingLock ||
            vaultState is VaultUiState.Editing ||
            unlockForHide ||
            pickMediaOpen
        val selecting = ready?.selecting == true
        val hideNav = overlay || selecting || searchOpen || vaultOpen
        val unlockedVault = vaultState as? VaultUiState.Unlocked

        AppBackHandler(enabled = pickMediaOpen && !albumNameOpen && !albumPickerOpen) {
            resetAlbumFlow()
        }
        AppBackHandler(enabled = searchOpen && !overlay) { searchOpen = false }
        AppBackHandler(enabled = unlockForHide) {
            galleryViewModel.cancelHideUnlock()
        }
        AppBackHandler(
            enabled = vaultOpen &&
                unlockedVault?.openedCollection != null &&
                unlockedVault?.selecting != true &&
                !overlay &&
                vaultState !is VaultUiState.Viewing,
        ) {
            vaultViewModel.closeCollection()
        }
        AppBackHandler(
            enabled = vaultOpen &&
                (unlockedVault?.selecting == true) &&
                !overlay,
        ) {
            vaultViewModel.clearSelection()
        }
        AppBackHandler(
            enabled = vaultOpen &&
                unlockedVault?.openedCollection == null &&
                unlockedVault?.selecting != true &&
                !overlay &&
                vaultState !is VaultUiState.Viewing,
        ) {
            vaultOpen = false
            vaultViewModel.lockVault()
            galleryViewModel.refresh()
        }
        AppBackHandler(
            enabled = tab == AppTab.VIDEOS &&
                videoAlbumKey != ALL_VIDEOS_KEY &&
                ready?.openedAlbum == null &&
                !overlay &&
                !selecting &&
                !searchOpen &&
                !vaultOpen,
        ) {
            videoAlbumKey = ALL_VIDEOS_KEY
        }
        AppBackHandler(
            enabled = ready?.openedAlbum != null &&
                tab != AppTab.VIDEOS &&
                !overlay &&
                !selecting &&
                !searchOpen &&
                !vaultOpen,
        ) {
            galleryViewModel.closeAlbum()
        }
        AppBackHandler(enabled = selecting && !overlay) {
            galleryViewModel.clearSelection()
        }
        AppBackHandler(enabled = vaultState is VaultUiState.ChangingLock) {
            vaultViewModel.cancelChangeLock()
        }
        AppBackHandler(enabled = vaultState is VaultUiState.Editing) {
            vaultViewModel.closeEditor()
        }
        AppBackHandler(enabled = vaultState is VaultUiState.Viewing) {
            vaultViewModel.closeViewer()
        }
        AppBackHandler(enabled = ready?.viewing != null && ready.editing == null && !unlockForHide && !pickMediaOpen) {
            galleryViewModel.closeViewer()
        }
        AppBackHandler(enabled = ready?.editing != null) {
            galleryViewModel.closeEditor()
        }

        AnimatedContent(
            targetState = overlayKey(ready, vaultState, searchOpen, vaultOpen, unlockForHide, pickMediaOpen),
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))) togetherWith
                    fadeOut(tween(120))
            },
            label = "overlay",
        ) { key ->
            when {
                key.startsWith("vault-auth") -> VaultAuthHost(
                    viewModel = vaultViewModel,
                    state = vaultState,
                )
                key.startsWith("vault-change") -> {
                    val changing = vaultState as? VaultUiState.ChangingLock
                    if (changing != null) {
                        VaultChangeLockScreen(
                            currentMethod = changing.currentMethod,
                            busy = changing.busy,
                            error = changing.error,
                            onChangePin = vaultViewModel::changeLockPin,
                            onChangePattern = vaultViewModel::changeLockPattern,
                            onCancel = vaultViewModel::cancelChangeLock,
                        )
                    }
                }
                key.startsWith("vault-edit") -> {
                    val editing = vaultState as? VaultUiState.Editing
                    if (editing != null) {
                        EditorScreen(
                            preview = editing.preview,
                            busy = editing.busy,
                            error = editing.error,
                            onEditChanged = vaultViewModel::previewEdit,
                            onSave = vaultViewModel::saveEdit,
                            onClose = vaultViewModel::closeEditor,
                        )
                    }
                }
                key.startsWith("edit") && ready?.editing != null -> EditorScreen(
                    preview = ready.editing.preview,
                    busy = ready.editing.busy,
                    error = ready.editing.error,
                    onEditChanged = galleryViewModel::previewEdit,
                    onSave = galleryViewModel::saveEdit,
                    onClose = galleryViewModel::closeEditor,
                )
                key.startsWith("pick-media") && ready != null -> AlbumMediaPickerScreen(
                    items = ready.allItems,
                    albumTitle = pendingAlbumName
                        ?: ready.albums.firstOrNull { it.customId == pendingTargetId }?.title
                        ?: "Álbum",
                    allowSkip = pendingAlbumName != null,
                    onConfirm = { ids ->
                        val name = pendingAlbumName
                        val target = pendingTargetId
                        val source = fromCustomAlbumId
                        resetAlbumFlow()
                        when {
                            name != null -> galleryViewModel.createAlbum(name, ids, source)
                            target != null -> galleryViewModel.moveToAlbum(target, ids, source)
                        }
                    },
                    onSkip = {
                        val name = pendingAlbumName
                        resetAlbumFlow()
                        if (name != null) galleryViewModel.createAlbum(name)
                    },
                    onDismiss = { resetAlbumFlow() },
                )
                key.startsWith("view") && ready?.viewing != null -> GalleryViewerScreen(
                    session = ready.viewing,
                    favoriteIds = ready.favorites,
                    onBack = galleryViewModel::closeViewer,
                    onPageChanged = galleryViewModel::showViewerIndex,
                    onShare = galleryViewModel::shareCurrent,
                    onEdit = galleryViewModel::openEditor,
                    onFavorite = { galleryViewModel.toggleFavorite(ready.viewing.item) },
                    onDelete = galleryViewModel::deleteCurrent,
                    onMoveToVault = galleryViewModel::moveCurrentToVault,
                    onAddToAlbum = {
                        val id = ready?.viewing?.item?.id
                        if (id != null) {
                            requestMoveToAlbum(listOf(id), ready?.openedAlbum?.customId)
                        }
                    },
                )
                key.startsWith("vault-view") -> {
                    val viewing = vaultState as? VaultUiState.Viewing
                    if (viewing != null) {
                        VaultViewerScreen(
                            state = viewing,
                            onBack = vaultViewModel::closeViewer,
                            onExport = vaultViewModel::exportCurrent,
                            onDelete = vaultViewModel::deleteCurrent,
                            onEdit = vaultViewModel::openEditor,
                            onPageChanged = vaultViewModel::showItemAt,
                        )
                    }
                }
                else -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (!hideNav) {
                            GaleriaBottomBar(
                                tab = tab,
                                onTab = { selectTab(it) },
                                onSearch = { searchOpen = true },
                            )
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        Column(Modifier.fillMaxSize()) {
                            if (selecting && !vaultOpen) {
                                val visibleIds = visibleGalleryIds(
                                    ready = ready,
                                    tab = tab,
                                    videoAlbumKey = videoAlbumKey,
                                )
                                SelectionBar(
                                    count = ready?.selectedIds?.size ?: 0,
                                    onClose = galleryViewModel::clearSelection,
                                    onShare = galleryViewModel::shareSelected,
                                    onAddToAlbum = {
                                        requestMoveToAlbum(
                                            ready?.selectedIds?.toList().orEmpty(),
                                            ready?.openedAlbum?.customId,
                                        )
                                    },
                                    onMoveToVault = galleryViewModel::moveSelectedToVault,
                                    onDelete = galleryViewModel::deleteSelected,
                                    onRemoveFromAlbum = ready?.openedAlbum?.customId?.let { collectionId ->
                                        {
                                            galleryViewModel.removeFromAlbum(
                                                collectionId,
                                                ready.selectedIds.toList(),
                                            )
                                        }
                                    },
                                    onSelectAll = {
                                        galleryViewModel.selectAll(visibleIds)
                                    },
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                when {
                                    searchOpen -> GalleryTabBody(
                                        state = galleryState,
                                        onRequestPermission = galleryViewModel::requestPermission,
                                    ) { current ->
                                        SearchScreen(
                                            items = current.allItems,
                                            favorites = current.favorites,
                                            selectedIds = current.selectedIds,
                                            selecting = current.selecting,
                                            onOpen = { item ->
                                                galleryViewModel.openItem(item, current.allItems)
                                            },
                                            onLongPress = galleryViewModel::startSelection,
                                            onToggleSelect = galleryViewModel::toggleSelect,
                                            onClose = { searchOpen = false },
                                        )
                                    }
                                    vaultOpen -> VaultHost(
                                        viewModel = vaultViewModel,
                                        state = vaultState,
                                        onLeave = {
                                            vaultOpen = false
                                            vaultViewModel.lockVault()
                                            galleryViewModel.refresh()
                                        },
                                    )
                                    ready?.openedAlbum != null && tab != AppTab.VIDEOS -> AlbumGridScreen(
                                        album = ready.openedAlbum,
                                        selectedIds = ready.selectedIds,
                                        selecting = ready.selecting,
                                        message = ready.message,
                                        onBack = galleryViewModel::closeAlbum,
                                        onOpenItem = { galleryViewModel.openItem(it, ready.openedAlbum.items) },
                                        onLongPress = galleryViewModel::startSelection,
                                        onToggleSelect = galleryViewModel::toggleSelect,
                                        onAddPhotos = ready.openedAlbum.customId?.let { id ->
                                            { requestPickMediaForAlbum(id) }
                                        },
                                        onRename = ready.openedAlbum.customId?.let {
                                            { renameAlbumOpen = true }
                                        },
                                        onDeleteAlbum = ready.openedAlbum.customId?.let {
                                            { deleteAlbumOpen = true }
                                        },
                                    )
                                    else -> when (tab) {
                                        AppTab.PHOTOS -> GalleryTabBody(
                                            state = galleryState,
                                            onRequestPermission = galleryViewModel::requestPermission,
                                        ) { current ->
                                            PhotosScreen(
                                                items = current.allItems,
                                                albums = current.albums,
                                                favorites = current.favorites,
                                                selectedIds = current.selectedIds,
                                                selecting = current.selecting,
                                                message = current.message,
                                                refreshing = refreshing,
                                                onRefresh = galleryViewModel::refresh,
                                                onOpen = { item, collection ->
                                                    galleryViewModel.openItem(item, collection)
                                                },
                                                onOpenMemory = { card ->
                                                    if (card.id == "videos") {
                                                        selectTab(AppTab.VIDEOS)
                                                    } else {
                                                        galleryViewModel.openAlbum(
                                                            Album(AlbumId.OTHER, card.title, card.items),
                                                        )
                                                    }
                                                },
                                                onLongPress = galleryViewModel::startSelection,
                                                onToggleSelect = galleryViewModel::toggleSelect,
                                                onCreateAlbum = { requestMoveToAlbum(emptyList()) },
                                                onOpenVault = requestVault,
                                            )
                                        }
                                        AppTab.VIDEOS -> GalleryTabBody(
                                            state = galleryState,
                                            onRequestPermission = galleryViewModel::requestPermission,
                                        ) { current ->
                                            VideosScreen(
                                                items = current.allItems,
                                                albums = current.albums,
                                                selectedIds = current.selectedIds,
                                                selecting = current.selecting,
                                                message = current.message,
                                                refreshing = refreshing,
                                                selectedAlbumKey = videoAlbumKey,
                                                onSelectAlbum = { videoAlbumKey = it },
                                                onRefresh = galleryViewModel::refresh,
                                                onOpen = { item, collection ->
                                                    galleryViewModel.openItem(item, collection)
                                                },
                                                onLongPress = galleryViewModel::startSelection,
                                                onToggleSelect = galleryViewModel::toggleSelect,
                                                onCreateAlbum = { requestMoveToAlbum(emptyList()) },
                                                onOpenVault = requestVault,
                                            )
                                        }
                                        AppTab.COLLECTIONS -> GalleryTabBody(
                                            state = galleryState,
                                            onRequestPermission = galleryViewModel::requestPermission,
                                        ) { current ->
                                            CollectionsScreen(
                                                albums = current.albums,
                                                allItems = current.allItems,
                                                favorites = current.favorites,
                                                message = current.message,
                                                onOpenAlbum = { album ->
                                                    if (album.title == VIDEOS_COLLECTION_TITLE && album.customId == null) {
                                                        selectTab(AppTab.VIDEOS)
                                                    } else {
                                                        galleryViewModel.openAlbum(album)
                                                    }
                                                },
                                                onCreateAlbum = { requestMoveToAlbum(emptyList()) },
                                                onOpenVault = requestVault,
                                            )
                                        }
                                        AppTab.CREATE -> GalleryTabBody(
                                            state = galleryState,
                                            onRequestPermission = galleryViewModel::requestPermission,
                                        ) { current ->
                                            val last = current.allItems.firstOrNull {
                                                it.mediaKind == GalleryMediaKind.IMAGE
                                            }
                                            val newest = current.allItems.maxOfOrNull { it.capturedAtEpochMs } ?: 0L
                                            val weekend = current.allItems.filter {
                                                newest - it.capturedAtEpochMs <= 3L * 24L * 60L * 60L * 1000L
                                            }
                                            CreateScreen(
                                                lastItem = last,
                                                weekendItems = weekend,
                                                onEditLast = {
                                                    if (last != null) galleryViewModel.editItem(last)
                                                },
                                                onOpenWeekend = {
                                                    galleryViewModel.openAlbum(
                                                        Album(AlbumId.OTHER, "Dump del fin de semana", weekend),
                                                    )
                                                },
                                                onCreateAlbum = { requestMoveToAlbum(emptyList()) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val renamingAlbum = ready?.openedAlbum?.takeIf { renameAlbumOpen && it.customId != null }
        if (renamingAlbum != null) {
            val collectionId = renamingAlbum.customId ?: return@GaleriaTheme
            AlbumNameDialog(
                title = "Renombrar álbum",
                confirmLabel = "Guardar",
                initialName = renamingAlbum.title,
                hint = "Cambia el nombre de esta colección.",
                onConfirm = { name ->
                    renameAlbumOpen = false
                    galleryViewModel.renameAlbum(collectionId, name)
                },
                onDismiss = { renameAlbumOpen = false },
            )
        }
        val deletingAlbum = ready?.openedAlbum?.takeIf { deleteAlbumOpen && it.customId != null }
        if (deletingAlbum != null) {
            val collectionId = deletingAlbum.customId ?: return@GaleriaTheme
            AlbumDeleteConfirmDialog(
                title = deletingAlbum.title,
                onConfirm = {
                    deleteAlbumOpen = false
                    galleryViewModel.deleteAlbum(collectionId)
                },
                onDismiss = { deleteAlbumOpen = false },
            )
        }
        if (albumNameOpen) {
            AlbumNameDialog(
                onConfirm = { name ->
                    albumNameOpen = false
                    if (pendingAlbumIds.isNotEmpty()) {
                        val ids = pendingAlbumIds
                        val source = fromCustomAlbumId
                        resetAlbumFlow()
                        galleryViewModel.createAlbum(name, ids, source)
                    } else if (currentReady()?.allItems.isNullOrEmpty()) {
                        resetAlbumFlow()
                        galleryViewModel.createAlbum(name)
                    } else {
                        pendingAlbumName = name
                        pendingTargetId = null
                        pickMediaOpen = true
                    }
                },
                onDismiss = { resetAlbumFlow() },
            )
        }
        if (albumPickerOpen) {
            AlbumPickerDialog(
                albums = customAlbums().filter { it.customId != fromCustomAlbumId },
                onPick = { id ->
                    albumPickerOpen = false
                    if (pendingAlbumIds.isNotEmpty()) {
                        val ids = pendingAlbumIds
                        val source = fromCustomAlbumId
                        resetAlbumFlow()
                        galleryViewModel.moveToAlbum(id, ids, source)
                    } else if (currentReady()?.allItems.isNullOrEmpty()) {
                        val album = customAlbums().firstOrNull { it.customId == id }
                        resetAlbumFlow()
                        if (album != null) galleryViewModel.openAlbum(album)
                    } else {
                        pendingTargetId = id
                        pendingAlbumName = null
                        pickMediaOpen = true
                    }
                },
                onCreateNew = { requestCreateAlbum(pendingAlbumIds) },
                onDismiss = { resetAlbumFlow() },
            )
        }
    }
}

@Composable
private fun GalleryTabBody(
    state: GalleryUiState,
    onRequestPermission: () -> Unit,
    content: @Composable (GalleryUiState.Ready) -> Unit,
) {
    when (state) {
        is GalleryUiState.NeedsPermission -> GalleryPermissionPane(onRequestPermission)
        is GalleryUiState.Loading -> GalleryLoadingPane()
        is GalleryUiState.Ready -> content(state)
    }
}

@Composable
private fun GaleriaBottomBar(
    tab: AppTab,
    onTab: (AppTab) -> Unit,
    onSearch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(end = 88.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomItem(
                selected = tab == AppTab.PHOTOS,
                label = "Fotos",
                selectedIcon = GaleriaIcons.PhotosFilled,
                unselectedIcon = GaleriaIcons.PhotosOutline,
                onClick = { onTab(AppTab.PHOTOS) },
                modifier = Modifier.weight(1f),
            )
            BottomItem(
                selected = tab == AppTab.VIDEOS,
                label = "Videos",
                selectedIcon = GaleriaIcons.VideosFilled,
                unselectedIcon = GaleriaIcons.VideosOutline,
                onClick = { onTab(AppTab.VIDEOS) },
                modifier = Modifier.weight(1f),
            )
            BottomItem(
                selected = tab == AppTab.COLLECTIONS,
                label = "Colecciones",
                selectedIcon = GaleriaIcons.CollectionsFilled,
                unselectedIcon = GaleriaIcons.CollectionsOutline,
                onClick = { onTab(AppTab.COLLECTIONS) },
                modifier = Modifier.weight(1f),
            )
            BottomItem(
                selected = tab == AppTab.CREATE,
                label = "Crear",
                selectedIcon = GaleriaIcons.Plus,
                unselectedIcon = GaleriaIcons.CreateOutline,
                onClick = { onTab(AppTab.CREATE) },
                modifier = Modifier.weight(1f),
            )
        }
        FloatingActionButton(
            onClick = onSearch,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp, bottom = 6.dp)
                .size(58.dp),
            shape = CircleShape,
            containerColor = AccentsLavender,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(GaleriaIcons.SearchOutline, contentDescription = "Buscar", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .height(64.dp)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Surface(
                onClick = onClick,
                color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                modifier = Modifier.size(width = 56.dp, height = 32.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun VaultHost(
    viewModel: VaultLockViewModel,
    state: VaultUiState,
    onLeave: () -> Unit,
) {
    when (state) {
        is VaultUiState.Loading -> GalleryLoadingPane()
        is VaultUiState.NeedsSetup -> VaultSetupScreen(
            onSetupPin = viewModel::setupPin,
            onSetupPattern = viewModel::setupPattern,
        )
        is VaultUiState.Locked -> VaultUnlockScreen(
            state = state,
            onUnlockPin = viewModel::unlockWithPin,
            onUnlockPattern = viewModel::unlockWithPattern,
            onUnlockBiometric = viewModel::unlockWithBiometric,
            onUsePin = viewModel::usePinFallback,
            onCancelBiometric = viewModel::cancelBiometricPrompt,
        )
        is VaultUiState.Unlocked -> VaultGridScreen(
            state = state,
            onOpenItem = { item, collection -> viewModel.openItem(item, collection) },
            onLeave = onLeave,
            onOpenCollection = viewModel::openCollection,
            onCloseCollection = viewModel::closeCollection,
            onEnableBiometric = viewModel::enableBiometric,
            onSkipBiometric = viewModel::skipBiometric,
            onChangeLock = viewModel::startChangeLock,
            onLongPress = viewModel::startSelection,
            onToggleSelect = viewModel::toggleSelect,
            onClearSelection = viewModel::clearSelection,
            onExportSelected = viewModel::exportSelected,
            onDeleteSelected = viewModel::deleteSelected,
            loadThumbnail = viewModel::loadThumbnail,
        )
        is VaultUiState.ChangingLock, is VaultUiState.Viewing, is VaultUiState.Editing -> Unit
    }
}

private fun overlayKey(
    ready: GalleryUiState.Ready?,
    vaultState: VaultUiState,
    searchOpen: Boolean,
    vaultOpen: Boolean,
    unlockForHide: Boolean,
    pickMediaOpen: Boolean,
): String = when {
    unlockForHide -> "vault-auth"
    vaultState is VaultUiState.ChangingLock -> "vault-change"
    vaultState is VaultUiState.Editing -> "vault-edit"
    ready?.editing != null -> "edit"
    pickMediaOpen -> "pick-media"
    ready?.viewing != null -> "view"
    vaultState is VaultUiState.Viewing -> "vault-view"
    searchOpen -> "search"
    vaultOpen -> "vault"
    else -> "main"
}

private fun visibleGalleryIds(
    ready: GalleryUiState.Ready?,
    tab: AppTab,
    videoAlbumKey: String,
): List<String> {
    if (ready == null) return emptyList()
    ready.openedAlbum?.let { return it.items.map { item -> item.id } }
    if (tab == AppTab.VIDEOS) {
        val videos = ready.allItems.filter { it.mediaKind == GalleryMediaKind.VIDEO }
        if (videoAlbumKey == ALL_VIDEOS_KEY) return videos.map { it.id }
        return classifyVideoAlbums(ready.albums, videos)
            .firstOrNull { videoAlbumKey(it) == videoAlbumKey }
            ?.items
            ?.map { it.id }
            .orEmpty()
    }
    return ready.allItems.map { it.id }
}

@Composable
private fun VaultAuthHost(
    viewModel: VaultLockViewModel,
    state: VaultUiState,
) {
    when (state) {
        is VaultUiState.Loading, is VaultUiState.Unlocked -> GalleryLoadingPane()
        is VaultUiState.NeedsSetup -> VaultSetupScreen(
            onSetupPin = viewModel::setupPin,
            onSetupPattern = viewModel::setupPattern,
        )
        is VaultUiState.Locked -> VaultUnlockScreen(
            state = state,
            onUnlockPin = viewModel::unlockWithPin,
            onUnlockPattern = viewModel::unlockWithPattern,
            onUnlockBiometric = viewModel::unlockWithBiometric,
            onUsePin = viewModel::usePinFallback,
            onCancelBiometric = viewModel::cancelBiometricPrompt,
        )
        is VaultUiState.ChangingLock, is VaultUiState.Viewing, is VaultUiState.Editing -> Unit
    }
}
