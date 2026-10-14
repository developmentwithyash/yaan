package com.yaan.core.data.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.repository.SyncRepository

class InMemorySyncRepository : SyncRepository {
    override suspend fun getSyncStatus(): YaanResult<String> {
        return YaanResult.Success("Online • Local Vault Synchronized")
    }

    override suspend fun triggerSync(): YaanResult<Unit> {
        return YaanResult.Success(Unit)
    }

    override suspend fun exportVaultCiphertext(): YaanResult<String> {
        return YaanResult.Success("{\"vault_version\":\"2.0\",\"format\":\"AES-GCM-256\",\"encrypted_at\":\"2026-10-14T00:00:00Z\"}")
    }

    override suspend fun purgeVault(): YaanResult<Unit> {
        return YaanResult.Success(Unit)
    }
}
