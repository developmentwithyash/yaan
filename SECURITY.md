# Security Policy

## Supported Versions

We actively provide security patches and updates for the following versions of YAAN:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## ✦ Threat Model & Privacy Invariants

YAAN is engineered with **Zero-Knowledge Privacy** as a foundational architecture:

* **Hardware-Backed Encryption**: Vault databases (`vault.db`) are encrypted with AES-GCM-256 using master keys managed by the Android Keystore or iOS Keychain.
* **Zero Plaintext at Rest**: No unencrypted user notes, tasks, or audio memos are written to disk.
* **Client-Side Redaction**: Before any consented cloud-assisted query, all personal names, phone numbers, and emails are scrubbed by `VaultRedactor` in RAM.
* **Stateless Blind Relay**: Cloud sync relays only transmit blinded, authenticated ciphertext envelopes (`encrypted_sync_envelopes`) without possessing decryption keys.

---

## ✦ Reporting a Vulnerability

We take the security of YAAN and our users' privacy with utmost seriousness.

If you believe you have discovered a vulnerability, security flaw, or cryptographic weakness:

1. **Do not create a public GitHub issue.**
2. Send an encrypted or confidential email to **`security@yaan.ai`**.
3. Include detailed steps to reproduce the vulnerability, including affected platform (Android/iOS/JVM) and code paths.
4. You will receive an initial acknowledgment within **48 hours**.
5. We will coordinate remediation and provide a timeline for publishing a security advisory.

Thank you for helping keep YAAN sovereign and secure.
