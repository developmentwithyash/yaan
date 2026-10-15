package com.yaan.core.data.crypto

import platform.Foundation.NSUserDefaults
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * iOS implementation of VaultKeyManager using private sandbox defaults.
 */
class IosVaultKeyManager : VaultKeyManager {
    private val defaults = NSUserDefaults.standardUserDefaults
    private var cachedKey: ByteArray? = null

    @OptIn(ExperimentalEncodingApi::class)
    override fun getOrCreateMasterKey(): ByteArray {
        cachedKey?.let { return it }

        val storedBase64 = defaults.stringForKey(PREF_KEY_MASTER)
        if (!storedBase64.isNullOrEmpty()) {
            val decoded = Base64.decode(storedBase64)
            if (decoded.size == 32) {
                cachedKey = decoded
                return decoded
            }
        }

        val newKey = Random.Default.nextBytes(32)
        val encoded = Base64.encode(newKey)
        defaults.setObject(encoded, PREF_KEY_MASTER)
        cachedKey = newKey
        return newKey
    }

    override fun shredKey() {
        cachedKey?.fill(0)
        cachedKey = null
        defaults.removeObjectForKey(PREF_KEY_MASTER)
    }

    companion object {
        private const val PREF_KEY_MASTER = "vault_master_key_v1"
    }
}
