package com.yaan.core.data.crypto

import java.io.File
import java.security.SecureRandom

/**
 * Desktop JVM Key Manager.
 * Stores master key securely in user home directory (~/.yaan/vault.key) or generates in-memory.
 */
class JvmVaultKeyManager : VaultKeyManager {
    private val keyFile: File by lazy {
        val yaanDir = File(System.getProperty("user.home"), ".yaan")
        if (!yaanDir.exists()) yaanDir.mkdirs()
        File(yaanDir, "vault.key")
    }

    override fun getOrCreateMasterKey(): ByteArray {
        return if (keyFile.exists() && keyFile.length() == 32L) {
            keyFile.readBytes()
        } else {
            val newKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            try {
                keyFile.writeBytes(newKey)
            } catch (_: Exception) {}
            newKey
        }
    }

    override fun shredKey() {
        if (keyFile.exists()) {
            keyFile.writeBytes(ByteArray(32)) // Zeroize
            keyFile.delete()
        }
    }
}
