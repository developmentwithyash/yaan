# YAAN Desktop Native Packaging Guide

This guide covers building and distributing native desktop installers and standalone binaries for **YAAN (Your Autonomous Action Navigator)** across macOS, Linux, and Windows using the Compose Multiplatform Gradle plugin.

---

## 1. Prerequisites

* **JDK:** OpenJDK or Eclipse Temurin **21+** (`java -version`).
* **Platform Packaging Utilities:**
  * **macOS:** Standard Xcode Command Line Tools (`xcode-select --install`) for DMG and PKG creation.
  * **Linux:** `dpkg` / `fakeroot` for `.deb`, `rpmbuild` for `.rpm`.
  * **Windows:** WiX Toolset v3 for `.msi`.

---

## 2. Packaging Commands

### macOS (Apple Silicon & Intel)
```bash
# Package macOS DMG disk image (output: composeApp/build/compose/binaries/main/dmg/)
./gradlew :composeApp:packageDmg

# Package macOS PKG installer (output: composeApp/build/compose/binaries/main/pkg/)
./gradlew :composeApp:packagePkg

# Build unpacked macOS .app bundle for local testing
./gradlew :composeApp:createDistributable
```

### Linux (Ubuntu, Debian, Fedora, Arch)
```bash
# Package Debian/Ubuntu installer (output: composeApp/build/compose/binaries/main/deb/)
./gradlew :composeApp:packageDeb

# Package RedHat/Fedora RPM installer (output: composeApp/build/compose/binaries/main/rpm/)
./gradlew :composeApp:packageRpm
```

### Windows (10 / 11)
```bash
# Package Windows MSI installer (output: composeApp/build/compose/binaries/main/msi/)
./gradlew :composeApp:packageMsi

# Package Windows EXE installer (output: composeApp/build/compose/binaries/main/exe/)
./gradlew :composeApp:packageExe
```

### Cross-Platform Universal Uber JAR
```bash
# Builds standalone executable desktop JAR (runnable with `java -jar`)
./gradlew :composeApp:packageUberJarForCurrentOS
```

---

## 3. Native Distribution Metadata

Configured in `composeApp/build.gradle.kts`:

| Field | Configuration | Notes |
| :--- | :--- | :--- |
| **Application ID** | `com.yaan.app.desktop` | Standard reverse-DNS namespace |
| **Package Name** | `YAAN` (Linux: `yaan-desktop`) | System-level desktop entry |
| **Version** | `1.0.0` | Semantic versioning |
| **Minimum macOS** | `12.0` (Monterey+) | Apple Silicon & x86_64 compatible |
| **Linux Menu Group** | `Utility` | XDG desktop specification |
| **Desktop Shortcuts** | Enabled (`shortcut = true`) | Start Menu & Desktop launcher |
