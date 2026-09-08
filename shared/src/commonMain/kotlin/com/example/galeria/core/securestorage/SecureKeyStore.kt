package com.example.galeria.core.securestorage

/**
 * Almacén de secretos en Keystore (Android) / Keychain (iOS).
 * La clave de wrapping no sale del hardware; [read]/[write] solo mueven
 * secretos de la app (p. ej. DEK envuelta, salt), nunca el PIN en texto plano.
 */
interface SecureKeyStore {

    fun write(id: String, secret: ByteArray)

    fun read(id: String): ByteArray?

    fun delete(id: String)

    fun contains(id: String): Boolean
}

private val secretIdPattern = Regex("^[A-Za-z0-9._-]{1,128}$")

internal fun requireValidSecretId(id: String) {
    require(secretIdPattern.matches(id)) { "Invalid secret id" }
}
