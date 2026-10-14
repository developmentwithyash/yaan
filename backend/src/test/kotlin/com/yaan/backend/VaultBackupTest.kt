package com.yaan.backend

import com.yaan.backend.vault.VaultBackupEnvelope
import com.yaan.backend.vault.VaultBackupService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultBackupTest {

    @Test
    fun `test vault backup upload and retrieval`() {
        val service = VaultBackupService()
        val userId = "exec-user-1"

        val envelope = VaultBackupEnvelope(
            backupId = "backup-001",
            userId = userId,
            ciphertextBlob = "ENCRYPTED_SQLITE_VAULT_BYTES",
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sizeBytes = 2048L,
            createdAtMs = 5000L
        )

        val uploadRes = service.saveBackup(envelope)
        assertTrue(uploadRes.success)
        assertEquals("backup-001", uploadRes.backupId)

        val latest = service.getLatestBackup(userId)
        assertNotNull(latest)
        assertEquals(2048L, latest.sizeBytes)
        assertEquals(envelope.sha256Checksum, latest.sha256Checksum)
    }

    @Test
    fun `test crypto-shredding purge emits verifiable sha-256 certificate`() {
        val service = VaultBackupService()
        val userId = "exec-user-to-shred"

        service.saveBackup(
            VaultBackupEnvelope("b-1", userId, "blob1", "hash1", 100L, 1000L)
        )
        service.saveBackup(
            VaultBackupEnvelope("b-2", userId, "blob2", "hash2", 100L, 2000L)
        )

        // Purge
        val cert = service.purgeAndShred(userId)
        assertEquals(userId, cert.userId)
        assertEquals(2, cert.deletedBackupsCount)
        assertTrue(cert.sha256Proof.length == 64) // Valid SHA-256 hex string
        assertEquals("GDPR-Article-17-Crypto-Shredding", cert.complianceStandard)

        // Verify backups are destroyed
        assertNull(service.getLatestBackup(userId))
    }
}
