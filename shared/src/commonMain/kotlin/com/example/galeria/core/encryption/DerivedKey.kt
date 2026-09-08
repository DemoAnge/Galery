package com.example.galeria.core.encryption

/**
 * Clave AES-256 (32 bytes) derivada del PIN.
 * No serializar ni loguear; [toString] no expone el material.
 */
class DerivedKey private constructor(private val bytes: ByteArray) {

    init {
        require(bytes.size == SIZE_BYTES) { "AES-256 key must be $SIZE_BYTES bytes" }
    }

    fun asBytes(): ByteArray = bytes.copyOf()

    fun clear() {
        bytes.fill(0)
    }

    override fun toString(): String = "DerivedKey(***)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedKey) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        const val SIZE_BYTES: Int = 32

        fun of(bytes: ByteArray): DerivedKey {
            require(bytes.size == SIZE_BYTES) { "AES-256 key must be $SIZE_BYTES bytes" }
            return DerivedKey(bytes.copyOf())
        }
    }
}
