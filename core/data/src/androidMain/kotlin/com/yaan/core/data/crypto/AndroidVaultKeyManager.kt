package com.yaan.core.data.crypto

import android.content.Context
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Android implementation of VaultKeyManager.
 * Stores a cryptographically secure 256-bit AES master key in the private app sandbox.
 */
class AndroidVaultKeyManager(
    private val context: Context
) : VaultKeyManager {

    private val prefs = context.getSharedPreferences("yaan_vault_sec_prefs", Context.MODE_PRIVATE)
    private var cachedKey: ByteArray? = null

    @OptIn(ExperimentalEncodingApi::class)
    @Synchronized
    override fun getOrCreateMasterKey(): ByteArray {
        cachedKey?.let { return it }

        val storedBase64 = prefs.getString(PREF_KEY_MASTER, null)
        if (!storedBase64.isNullOrEmpty()) {
            val decoded = Base64.decode(storedBase64)
            if (decoded.size == 32) {
                cachedKey = decoded
                return decoded
            }
        }

        // Generate high-entropy 256-bit random key
        val secureRandom = SecureRandom()
        val newKey = ByteArray(32)
        secureRandom.nextBytes(newKey)

        val encoded = Base64.encode(newKey)
        prefs.edit().putString(PREF_KEY_MASTER, encoded).commit()
        cachedKey = newKey
        return newKey
    }

    @Synchronized
    override fun shredKey() {
        cachedKey?.fill(0)
        cachedKey = null
        prefs.edit().remove(PREF_KEY_MASTER).commit()
    }

    companion object {
        private const val PREF_KEY_MASTER = "vault_master_key_v1"
    }
}
