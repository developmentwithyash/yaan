# Offline Encrypted Vault Backup & Restore (`.yaanvault`)

**Specification Version:** 2.0  
**Algorithm:** `PBKDF2-HMAC-SHA256:AES-256-GCM`  
**File Extension:** `.yaanvault`  

---

## 1. Overview & Threat Model

YAAN provides an offline encrypted export format enabling users to securely backup and migrate their personal vault (notes, tasks, vector memory indices) across devices without relying on third-party cloud sync relays.

### Core Security Guarantees
1. **Zero-Knowledge Decryption**: The master vault password is never transmitted over a network or stored on disk.
2. **Key Stretching**: 32-byte (256-bit) encryption keys are derived using PBKDF2-HMAC-SHA256 with a unique 16-byte random salt and 10,000 iterations.
3. **Authenticated Ciphertext**: The archive is encrypted using AES-GCM-256 with a unique 12-byte initialization vector (IV) and a 128-bit authentication tag.
4. **Pre-Decryption Integrity Check**: A SHA-256 checksum over the raw ciphertext is verified before any cryptographic decryption attempt, immediately rejecting tampered or corrupted bundles.

---

## 2. Bundle Data Format Schema

The export archive is serialized as a JSON envelope:

```json
{
  "formatVersion": "2.0",
  "algorithm": "PBKDF2-HMAC-SHA256:AES-256-GCM",
  "saltHex": "4a7f9b...",
  "iterations": 10000,
  "ivHex": "1a2b3c4d5e6f...",
  "ciphertextHex": "cafebabe...",
  "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "exportedAtEpochMs": 1776500000000,
  "notesCount": 42,
  "tasksCount": 18,
  "schemaVersion": "2.0"
}
```

---

## 3. Programmatic Usage

### Creating an Encrypted Bundle
```kotlin
val manager = VaultBundleManager()

val exportResult = manager.createEncryptedBundle(
    notes = noteList,
    tasks = taskList,
    password = userMasterPassword,
    timestampMs = System.currentTimeMillis()
)

if (exportResult is YaanResult.Success) {
    val fileJson = manager.serializeBundle(exportResult.data)
    saveToFile("vault_backup.yaanvault", fileJson)
}
```

### Restoring from an Encrypted Bundle
```kotlin
val manager = VaultBundleManager()
val bundleResult = manager.deserializeBundle(fileJson)

if (bundleResult is YaanResult.Success) {
    val restoreResult = manager.restoreBundle(
        bundle = bundleResult.data,
        password = userMasterPassword
    )
    
    when (restoreResult) {
        is YaanResult.Success -> {
            val archive = restoreResult.data
            // Import archive.notes and archive.tasks into local encrypted repository
        }
        is YaanResult.Failure -> {
            // Handle incorrect password or tampered bundle error
        }
    }
}
```
