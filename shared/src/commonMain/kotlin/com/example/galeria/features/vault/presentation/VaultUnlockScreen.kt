package com.example.galeria.features.vault.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.galeria.features.vault.domain.LockMethod
import com.example.galeria.ui.GaleriaIcons

@Composable
fun VaultUnlockScreen(
    state: VaultUiState.Locked,
    onUnlockPin: (CharArray) -> Unit,
    onUnlockPattern: (IntArray) -> Unit,
    onUnlockBiometric: () -> Unit,
    onUsePin: () -> Unit,
    onCancelBiometric: () -> Unit = {},
) {
    val canUseFingerprint = state.biometricEnabled && state.biometricAvailable
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (canUseFingerprint) GaleriaIcons.Fingerprint else GaleriaIcons.VaultOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Vault",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            when {
                canUseFingerprint && state.method == LockMethod.PIN ->
                    "Huella o PIN de ${LockMethod.PIN_DIGITS} dígitos"
                canUseFingerprint -> "Huella o patrón"
                state.method == LockMethod.PIN -> "PIN de ${LockMethod.PIN_DIGITS} dígitos"
                else -> "Dibuja tu patrón"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        if (canUseFingerprint) {
            Button(onClick = onUnlockBiometric) {
                Text(if (state.busy) "Esperando huella…" else "Usar huella")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onUsePin) {
                Text("Usar PIN")
            }
            Spacer(Modifier.height(16.dp))
        }
        when (state.method) {
            LockMethod.PIN -> {
                var pin by remember { mutableStateOf("") }
                LaunchedEffect(pin) {
                    if (pin.length == LockMethod.PIN_DIGITS && !state.busy) {
                        val chars = pin.toCharArray()
                        pin = ""
                        onUnlockPin(chars)
                    }
                }
                VaultPinPad(
                    value = pin,
                    enabled = !state.busy || (canUseFingerprint && !state.pinFallback),
                    onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                    onSubmit = {
                        if (pin.length == LockMethod.PIN_DIGITS) {
                            val chars = pin.toCharArray()
                            pin = ""
                            onUnlockPin(chars)
                        }
                    },
                )
            }
            LockMethod.PATTERN -> {
                var cells by remember { mutableStateOf(listOf<Int>()) }
                VaultPatternPad(
                    selected = cells,
                    enabled = !state.busy || (canUseFingerprint && !state.pinFallback),
                    onChange = { cells = it },
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (cells.size >= LockMethod.PATTERN_MIN_CELLS) {
                            onUnlockPattern(cells.toIntArray())
                            cells = emptyList()
                        }
                    },
                    enabled = !state.busy && cells.size >= LockMethod.PATTERN_MIN_CELLS,
                ) {
                    Text("Desbloquear")
                }
                TextButton(onClick = { cells = emptyList() }, enabled = !state.busy) {
                    Text("Borrar")
                }
            }
        }
    }
}

@Composable
internal fun VaultPinPad(
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(LockMethod.PIN_DIGITS) { index ->
            val filled = index < value.length
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) scheme.primary else scheme.surfaceContainerHighest)
                    .border(1.dp, scheme.outline, CircleShape),
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )
    keys.forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            row.forEach { label ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (label.isEmpty()) scheme.background
                            else scheme.surfaceContainerHighest,
                        )
                        .then(
                            if (label.isEmpty()) Modifier
                            else Modifier.border(1.dp, scheme.outline, CircleShape),
                        )
                        .clickable(enabled = enabled && label.isNotEmpty()) {
                            when (label) {
                                "⌫" -> if (value.isNotEmpty()) onChange(value.dropLast(1))
                                else -> if (value.length < LockMethod.PIN_DIGITS) {
                                    onChange(value + label)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (label.isNotEmpty()) {
                        Text(
                            label,
                            style = MaterialTheme.typography.headlineSmall,
                            color = scheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VaultPatternPad(
    selected: List<Int>,
    enabled: Boolean,
    onChange: (List<Int>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    val isOn = index in selected
                    val scheme = MaterialTheme.colorScheme
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isOn) scheme.primary else scheme.surfaceContainerHighest)
                            .border(1.dp, if (isOn) scheme.primary else scheme.outline, CircleShape)
                            .clickable(enabled = enabled) {
                                if (index !in selected) onChange(selected + index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isOn) {
                            Text(
                                "${selected.indexOf(index) + 1}",
                                color = scheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
