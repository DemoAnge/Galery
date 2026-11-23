package com.example.galeria.features.vault.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galeria.features.vault.domain.LockMethod
import com.example.galeria.features.vault.domain.patternCellsToSecret
import com.example.galeria.ui.GaleriaIcons

private enum class ChangeLockStep {
    Current,
    Choose,
    PinEnter,
    PinConfirm,
    PatternEnter,
    PatternConfirm,
}

@Composable
fun VaultChangeLockScreen(
    currentMethod: LockMethod,
    busy: Boolean,
    error: String?,
    onChangePin: (current: CharArray, pin: CharArray) -> Unit,
    onChangePattern: (current: CharArray, cells: IntArray) -> Unit,
    onCancel: () -> Unit,
) {
    var step by remember { mutableStateOf(ChangeLockStep.Current) }
    var currentSecret by remember { mutableStateOf<CharArray?>(null) }
    var firstPin by remember { mutableStateOf("") }
    var firstPattern by remember { mutableStateOf(listOf<Int>()) }
    var localError by remember { mutableStateOf<String?>(null) }
    val shownError = localError ?: error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = GaleriaIcons.VaultOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Cambiar candado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "El contenido se queda. Solo cambia el PIN o el patrón.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (shownError != null) {
            Text(shownError, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        when (step) {
            ChangeLockStep.Current -> {
                Text(
                    if (currentMethod == LockMethod.PIN) {
                        "PIN actual de ${LockMethod.PIN_DIGITS} dígitos"
                    } else {
                        "Patrón actual"
                    },
                )
                if (currentMethod == LockMethod.PIN) {
                    var pin by remember { mutableStateOf("") }
                    LaunchedEffect(pin) {
                        if (pin.length == LockMethod.PIN_DIGITS && !busy) {
                            currentSecret = pin.toCharArray()
                            pin = ""
                            step = ChangeLockStep.Choose
                        }
                    }
                    VaultPinPad(
                        value = pin,
                        enabled = !busy,
                        onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                        onSubmit = {},
                    )
                } else {
                    var cells by remember { mutableStateOf(listOf<Int>()) }
                    VaultPatternPad(selected = cells, enabled = !busy, onChange = { cells = it })
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            currentSecret = patternCellsToSecret(cells.toIntArray())
                            step = ChangeLockStep.Choose
                        },
                        enabled = !busy && cells.size >= LockMethod.PATTERN_MIN_CELLS,
                    ) { Text("Continuar") }
                }
                TextButton(onClick = onCancel, enabled = !busy) { Text("Cancelar") }
            }
            ChangeLockStep.Choose -> {
                Button(onClick = { step = ChangeLockStep.PinEnter; localError = null }) {
                    Text("Nuevo PIN de ${LockMethod.PIN_DIGITS} dígitos")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { step = ChangeLockStep.PatternEnter; localError = null }) {
                    Text("Nuevo patrón")
                }
                TextButton(onClick = onCancel, enabled = !busy) { Text("Cancelar") }
            }
            ChangeLockStep.PinEnter -> {
                var pin by remember { mutableStateOf("") }
                LaunchedEffect(pin) {
                    if (pin.length == LockMethod.PIN_DIGITS) {
                        firstPin = pin
                        pin = ""
                        step = ChangeLockStep.PinConfirm
                    }
                }
                Text("Elige el PIN nuevo")
                VaultPinPad(
                    value = pin,
                    enabled = !busy,
                    onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                    onSubmit = {},
                )
            }
            ChangeLockStep.PinConfirm -> {
                var pin by remember { mutableStateOf("") }
                LaunchedEffect(pin) {
                    if (pin.length != LockMethod.PIN_DIGITS || busy) return@LaunchedEffect
                    if (pin != firstPin) {
                        localError = "Los PIN no coinciden"
                        firstPin = ""
                        pin = ""
                        step = ChangeLockStep.PinEnter
                        return@LaunchedEffect
                    }
                    val current = currentSecret ?: return@LaunchedEffect
                    onChangePin(current.copyOf(), pin.toCharArray())
                    pin = ""
                }
                Text("Confirma el PIN nuevo")
                VaultPinPad(
                    value = pin,
                    enabled = !busy,
                    onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                    onSubmit = {},
                )
            }
            ChangeLockStep.PatternEnter -> {
                var cells by remember { mutableStateOf(listOf<Int>()) }
                Text("Dibuja el patrón nuevo")
                VaultPatternPad(selected = cells, enabled = !busy, onChange = { cells = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        firstPattern = cells
                        step = ChangeLockStep.PatternConfirm
                    },
                    enabled = !busy && cells.size >= LockMethod.PATTERN_MIN_CELLS,
                ) { Text("Continuar") }
            }
            ChangeLockStep.PatternConfirm -> {
                var cells by remember { mutableStateOf(listOf<Int>()) }
                Text("Repite el patrón nuevo")
                VaultPatternPad(selected = cells, enabled = !busy, onChange = { cells = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (cells != firstPattern) {
                            localError = "Los patrones no coinciden"
                            firstPattern = emptyList()
                            step = ChangeLockStep.PatternEnter
                        } else {
                            val current = currentSecret ?: return@Button
                            onChangePattern(current.copyOf(), cells.toIntArray())
                        }
                    },
                    enabled = !busy && cells.size >= LockMethod.PATTERN_MIN_CELLS,
                ) { Text("Guardar") }
            }
        }
    }
}
