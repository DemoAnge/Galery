package com.example.galeria.features.vault.data

import com.example.galeria.core.CoreMediaKind
import com.example.galeria.core.HideInVault
import com.example.galeria.core.PublicMediaRef
import com.example.galeria.features.vault.domain.ExternalMediaSource
import com.example.galeria.features.vault.domain.MoveToVaultUseCase
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.domain.VaultRepository

class VaultHideInVault(
    private val repository: VaultRepository,
) : HideInVault {

    private val moveToVault = MoveToVaultUseCase(repository)

    override fun isReady(): Boolean = repository.canHideWithoutUnlock()

    override fun isConfigured(): Boolean = repository.isLockConfigured()

    override fun hide(media: PublicMediaRef) {
        hideAll(listOf(media))
    }

    override fun hideAll(media: List<PublicMediaRef>) {
        if (media.isEmpty()) return
        moveToVault(
            media.map { item ->
                ExternalMediaSource(
                    publicUri = item.publicUri,
                    displayName = item.displayName,
                    mediaKind = when (item.mediaKind) {
                        CoreMediaKind.IMAGE -> VaultMediaKind.IMAGE
                        CoreMediaKind.VIDEO -> VaultMediaKind.VIDEO
                    },
                    capturedAtEpochMs = item.capturedAtEpochMs,
                    sizeBytes = item.sizeBytes,
                    albumLabel = item.albumLabel,
                )
            },
        )
    }
}

object VaultComponent {
    val repository: VaultRepository by lazy { createVaultRepository() }
    val hideInVault: HideInVault by lazy { VaultHideInVault(repository) }
}

fun createHideInVault(): HideInVault = VaultComponent.hideInVault
