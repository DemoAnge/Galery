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
import com.example.galeria.ui.GaleriaIcons

private enum class SetupStep {
    Choose,
    PinEnter,
    PinConfirm,
    PatternEnter,
    PatternConfirm,
}

@Composable
fun VaultSetupScreen(
    onSetupPin: (CharArray) -> Unit,
    onSetupPattern: (IntArray) -> Unit,
) {
    var step by remember { mutableStateOf(SetupStep.Choose) }
    var firstPin by remember { mutableStateOf("") }
    var firstPattern by remember { mutableStateOf(listOf<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }

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
        Text(
            "Crear Vault",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Primero un PIN de ${LockMethod.PIN_DIGITS} dígitos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        when (step) {
            SetupStep.Choose -> {
                Button(onClick = { step = SetupStep.PinEnter; error = null }) {
                    Text("Crear PIN de ${LockMethod.PIN_DIGITS} dígitos")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { step = SetupStep.PatternEnter; error = null }) {
                    Text("Usar patrón")
                }
            }
            SetupStep.PinEnter -> {
                Text(
                    "Elige un PIN de ${LockMethod.PIN_DIGITS} dígitos",
                    color = MaterialTheme.colorScheme.onBackground,
                )
                var pin by remember { mutableStateOf("") }
                LaunchedEffect(pin) {
                    if (pin.length == LockMethod.PIN_DIGITS) {
                        firstPin = pin
                        pin = ""
                        step = SetupStep.PinConfirm
                    }
                }
                VaultPinPad(
                    value = pin,
                    enabled = true,
                    onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                    onSubmit = {},
                )
                TextButton(onClick = { step = SetupStep.Choose; pin = "" }) { Text("Atrás") }
            }
            SetupStep.PinConfirm -> {
                Text(
                    "Confirma el PIN de ${LockMethod.PIN_DIGITS} dígitos",
                    color = MaterialTheme.colorScheme.onBackground,
                )
                var pin by remember { mutableStateOf("") }
                LaunchedEffect(pin) {
                    if (pin.length != LockMethod.PIN_DIGITS) return@LaunchedEffect
                    if (pin == firstPin) {
                        val chars = pin.toCharArray()
                        firstPin = ""
                        pin = ""
                        onSetupPin(chars)
                    } else {
                        error = "Los PIN no coinciden"
                        pin = ""
                        firstPin = ""
                        step = SetupStep.PinEnter
                    }
                }
                VaultPinPad(
                    value = pin,
                    enabled = true,
                    onChange = { pin = it.take(LockMethod.PIN_DIGITS) },
                    onSubmit = {},
                )
            }
            SetupStep.PatternEnter -> {
                Text("Dibuja un patrón de al menos ${LockMethod.PATTERN_MIN_CELLS} puntos")
                var cells by remember { mutableStateOf(listOf<Int>()) }
                VaultPatternPad(selected = cells, enabled = true, onChange = { cells = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        firstPattern = cells
                        step = SetupStep.PatternConfirm
                    },
                    enabled = cells.size >= LockMethod.PATTERN_MIN_CELLS,
                ) { Text("Continuar") }
                TextButton(onClick = { cells = emptyList() }) { Text("Borrar") }
                TextButton(onClick = { step = SetupStep.Choose }) { Text("Atrás") }
            }
            SetupStep.PatternConfirm -> {
                Text("Repite el patrón")
                var cells by remember { mutableStateOf(listOf<Int>()) }
                VaultPatternPad(selected = cells, enabled = true, onChange = { cells = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (cells == firstPattern) {
                            onSetupPattern(cells.toIntArray())
                        } else {
                            error = "Los patrones no coinciden"
                            firstPattern = emptyList()
                            step = SetupStep.PatternEnter
                        }
                    },
                    enabled = cells.size >= LockMethod.PATTERN_MIN_CELLS,
                ) { Text("Confirmar") }
                TextButton(onClick = { cells = emptyList() }) { Text("Borrar") }
            }
        }
    }
}
