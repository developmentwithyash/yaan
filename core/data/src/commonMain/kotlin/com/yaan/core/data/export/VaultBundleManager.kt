package com.yaan.core.data.export

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.AesGcmYaanCipher
import com.yaan.core.data.crypto.VaultKeyDeriver
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Enterprise manager for exporting and restoring encrypted offline .yaanvault archives.
 * Enforces:
 * 1. PBKDF2-HMAC-SHA256 password-derived 256-bit AES master encryption keys.
 * 2. Authenticated AES-GCM-256 payload encryption with 12-byte IVs.
 * 3. Pre-decryption SHA-256 checksum verification to detect tampering and corruptions.
 * 4. Schema versioning ("2.0") and integrity validation.
 */
class VaultBundleManager(
    private val cipher: AesGcmYaanCipher = AesGcmYaanCipher(),
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    /**
     * Creates an encrypted .yaanvault bundle containing the specified notes and tasks.
     * Uses a user-supplied passphrase to derive a strong AES-GCM encryption key.
     */
    suspend fun createEncryptedBundle(
        notes: List<Note>,
        tasks: List<Task>,
        password: String,
        timestampMs: Long,
        iterations: Int = EncryptedVaultBundle.DEFAULT_PBKDF2_ITERATIONS
    ): YaanResult<EncryptedVaultBundle> {
        if (password.isBlank()) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "password",
                    message = "Vault backup password must not be blank."
                )
            )
        }

        return try {
            val salt = VaultKeyDeriver.generateRandomBytes(16)
            val derivedKey = VaultKeyDeriver.deriveKey(
                password = password,
                salt = salt,
                iterations = iterations,
                keyLengthBytes = 32
            )

            val archive = VaultExportArchive(
                exportVersion = "2.0",
                exportedAtEpochMs = timestampMs,
                notesCount = notes.size,
                tasksCount = tasks.size,
                notes = notes,
                tasks = tasks
            )
            val plainText = json.encodeToString(archive)

            val encResult = cipher.encrypt(plainText, derivedKey)
            if (encResult is YaanResult.Failure) {
                return YaanResult.Failure(encResult.error)
            }

            val cipherBytesWithIv = (encResult as YaanResult.Success<ByteArray>).data
            if (cipherBytesWithIv.size < 12) {
                return YaanResult.Failure(YaanError.CryptoError("Encryption failed: payload too short"))
            }

            val iv = cipherBytesWithIv.copyOfRange(0, 12)
            val ciphertext = cipherBytesWithIv.copyOfRange(12, cipherBytesWithIv.size)
            val checksum = VaultKeyDeriver.sha256Hex(ciphertext)

            val bundle = EncryptedVaultBundle(
                formatVersion = EncryptedVaultBundle.BUNDLE_FORMAT_VERSION,
                algorithm = "PBKDF2-HMAC-SHA256:AES-256-GCM",
                saltHex = salt.toHex(),
                iterations = iterations,
                ivHex = iv.toHex(),
                ciphertextHex = ciphertext.toHex(),
                checksumSha256 = checksum,
                exportedAtEpochMs = timestampMs,
                notesCount = notes.size,
                tasksCount = tasks.size,
                schemaVersion = "2.0"
            )

            YaanResult.Success(bundle)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.CryptoError("Failed to create encrypted bundle: ${e.message}"))
        }
    }

    /**
     * Validates bundle integrity (format version and SHA-256 checksum) and decrypts the archive
     * using the user's master passphrase.
     */
    suspend fun restoreBundle(
        bundle: EncryptedVaultBundle,
        password: String
    ): YaanResult<VaultExportArchive> {
        if (password.isBlank()) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "password",
                    message = "Password must not be blank."
                )
            )
        }

        if (bundle.formatVersion != EncryptedVaultBundle.BUNDLE_FORMAT_VERSION && bundle.formatVersion != "1.0") {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "formatVersion",
                    message = "Unsupported vault bundle format version: ${bundle.formatVersion}"
                )
            )
        }

        return try {
            val ciphertext = try {
                bundle.ciphertextHex.fromHex()
            } catch (e: Exception) {
                return YaanResult.Failure(
                    YaanError.ValidationError(
                        field = "ciphertextHex",
                        message = "Invalid ciphertext hex encoding in bundle."
                    )
                )
            }

            val calculatedChecksum = VaultKeyDeriver.sha256Hex(ciphertext)
            if (!calculatedChecksum.equals(bundle.checksumSha256, ignoreCase = true)) {
                return YaanResult.Failure(
                    YaanError.ValidationError(
                        field = "checksumSha256",
                        message = "Integrity check failed: Checksum mismatch. Expected ${bundle.checksumSha256}, got $calculatedChecksum"
                    )
                )
            }

            val salt = bundle.saltHex.fromHex()
            val derivedKey = VaultKeyDeriver.deriveKey(
                password = password,
                salt = salt,
                iterations = bundle.iterations,
                keyLengthBytes = 32
            )

            val iv = bundle.ivHex.fromHex()
            val cipherPayload = ByteArray(iv.size + ciphertext.size)
            iv.copyInto(cipherPayload, 0)
            ciphertext.copyInto(cipherPayload, iv.size)

            val decResult = cipher.decrypt(cipherPayload, derivedKey)
            if (decResult is YaanResult.Failure) {
                return YaanResult.Failure(decResult.error)
            }

            val plainJson = (decResult as YaanResult.Success<String>).data
            val archive = json.decodeFromString<VaultExportArchive>(plainJson)
            YaanResult.Success(archive)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.CryptoError("Decryption failed: ${e.message}"))
        }
    }

    /**
     * Serializes an EncryptedVaultBundle into a portable JSON string ready to save as a .yaanvault file.
     */
    fun serializeBundle(bundle: EncryptedVaultBundle): String {
        return json.encodeToString(bundle)
    }

    /**
     * Deserializes a .yaanvault JSON file string into an EncryptedVaultBundle data model.
     */
    fun deserializeBundle(bundleJson: String): YaanResult<EncryptedVaultBundle> {
        return try {
            val bundle = json.decodeFromString<EncryptedVaultBundle>(bundleJson)
            YaanResult.Success(bundle)
        } catch (e: Exception) {
            YaanResult.Failure(
                YaanError.ValidationError(
                    field = "bundleJson",
                    message = "Failed to deserialize vault bundle: ${e.message}"
                )
            )
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0) { "Hex string must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
