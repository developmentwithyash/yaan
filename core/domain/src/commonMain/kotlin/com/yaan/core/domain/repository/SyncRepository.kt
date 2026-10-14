package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult

interface SyncRepository {
    suspend fun getSyncStatus(): YaanResult<String>
    suspend fun triggerSync(): YaanResult<Unit>
    suspend fun exportVaultCiphertext(): YaanResult<String>
    suspend fun purgeVault(): YaanResult<Unit>
}
