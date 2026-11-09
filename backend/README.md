# ⚡ YAAN Backend — Sovereign CRDT Sync Relay & Vault Backup

The YAAN backend is a high-performance, asynchronous Ktor service that acts as an **end-to-end encrypted (E2EE) zero-knowledge relay** for cross-device state synchronization and vault backups.

---

## 🔒 Security Invariants

1. **Zero-Knowledge Blind Relay:** The relay acts exclusively on encrypted CRDT payloads. It holds no encryption keys, decrypts zero user data, and cannot inspect vault contents.
2. **Deterministic State:** State replication uses convergent CRDT delta-relays over WebSockets.
3. **Container Security:** Runs as a non-privileged `yaan` system user in a hardened Alpine JRE container.

---

## 🐳 Quickstart with Docker Compose

To deploy your own self-hosted sync relay on any VPS, home server, or local machine:

```bash
# Clone the repository
git clone https://github.com/developmentwithyash/yaan.git
cd yaan

# Launch the containerized relay
docker compose up -d
```

### Verify Container Health

```bash
# Check service health
curl -s http://localhost:8080/healthz | jq .
```

Expected response:
```json
{
  "status": "HEALTHY",
  "service": "YAAN Asymmetric Acceleration Hub",
  "version": "2.0.0-GOLD-MASTER",
  "invariants": [
    "Deterministic State Decoupled from Probabilistic AI",
    "Zero-Knowledge E2EE Blind Relay",
    "Sub-150ms Interaction Ceiling",
    "Pre-computed Async Nightly Intelligence"
  ]
}
```

---

## 🛠️ Local Development (Without Docker)

Run the backend server directly with Gradle:

```bash
# Start server in development mode
./gradlew :backend:run

# Run backend unit tests
./gradlew :backend:test
```
