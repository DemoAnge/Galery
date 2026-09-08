package com.example.galeria.features.vault.domain

/**
 * Método obligatorio de desbloqueo. La biometría es un atajo de presentación,
 * nunca un [LockMethod] por sí sola.
 */
enum class LockMethod {
    PIN,
    PATTERN,
    ;

    companion object {
        const val PIN_DIGITS: Int = 6
        const val PIN_MIN_DIGITS: Int = PIN_DIGITS
        const val PIN_MAX_DIGITS: Int = PIN_DIGITS
        const val PATTERN_CELL_COUNT: Int = 9
        const val PATTERN_MIN_CELLS: Int = 4
    }
}

internal fun patternCellsToSecret(cells: IntArray): CharArray =
    CharArray(cells.size) { index -> ('0'.code + cells[index]).toChar() }
