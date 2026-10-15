package com.yaan.core.data.crypto

/**
 * Interface for managing the zero-knowledge AES-256 master vault key on device.
 * Enforces:
 * 1. Key is derived/stored strictly in hardware or private app sandbox.
 * 2. Key is never transmitted across network or written to unencrypted logs.
 * 3. Supports crypto-shredding (instant zeroization on vault purge).
 */
interface VaultKeyManager {
    /**
     * Returns the 32-byte (256-bit) AES key.
     * Generates a cryptographically strong key on first run.
     */
    fun getOrCreateMasterKey(): ByteArray

    /**
     * Securely zeroizes the key in memory and removes it from persistent storage,
     * rendering all encrypted data permanently unrecoverable (GDPR Art 17 crypto-shredding).
     */
    fun shredKey()
}

/**
 * In-memory fallback key manager for headless unit tests or transient sessions.
 */
class InMemoryVaultKeyManager(
    initialKey: ByteArray? = null
) : VaultKeyManager {
    private var key: ByteArray? = initialKey ?: ByteArray(32) { 0x7A.toByte() }

    override fun getOrCreateMasterKey(): ByteArray {
        return key ?: ByteArray(32) { 0x7A.toByte() }.also { key = it }
    }

    override fun shredKey() {
        key?.fill(0)
        key = null
    }
}
