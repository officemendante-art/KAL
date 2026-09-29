# Device Build Specifications & Compatibility

This document outlines the device environment, compatibility analysis, and build configuration for **Calculator-You** matching the target phone.

---

## 1. Target Device Profile (From Phone Info)

| Parameter | Device Value |
| :--- | :--- |
| **Device Model** | POCO X7 Pro |
| **Operating System** | Xiaomi HyperOS `3.0.302.0` (`3.0.302.0.WOJINXM.C07`) |
| **Android Version** | Android 16 (`BP2A.250605.031.A3`) |
| **Android Security Patch** | 2026-07-01 |
| **CPU Architecture** | ARM64-v8a (64-bit) |
| **Storage Available** | ~237.4 GB free of 256 GB |

---

## 2. Target App Profile (From App Info)

| Parameter | Configuration in Repository | Installed App on Device | Status |
| :--- | :--- | :--- | :--- |
| **App Name** | Calculator | Calculator | Exact Match |
| **Application ID** | `com.marktka.calculatorYou` | `com.marktka.calculatorYou` | Exact Match |
| **Version Name** | `3.1.2` | `3.1.2` | Exact Match |
| **Version Code** | `33` | `33` | Exact Match |
| **Source / Distribution**| Open-source repository | F-Droid | Compatible |

---

## 3. SDK & OS Compatibility Analysis

- **`minSdk: 27` (Android 8.1 Oreo)**: Your device is on Android 16 (API 36). Since $36 \ge 27$, the minimum OS version is fully satisfied.
- **`targetSdk: 34` (Android 14)**: Fully compatible with Android 16 runtime. Android 16 runs apps targeting SDK 34 without legacy restriction warnings.
- **`compileSdk: 34`**: Uses Android SDK platform tools and compilation stubs verified against local build tools.
- **Architectural ABI**: Pure JVM/Kotlin + AndroidX Jetpack dependencies with no native `.so` library constraints, runs natively on ARM64-v8a.
- **Installation Note**: Because the current app on your phone was installed via F-Droid, Android requires that any update installed over an existing APK share the same digital signature. If building with a local debug key, you will need to uninstall the F-Droid build first or sign with the original key.

---

## 4. Build Environment Readiness

- [x] **Repository Source**: Cloned at `master` branch.
- [x] **Android SDK**: `C:\Users\DANTE\AppData\Local\Android\Sdk` configured in `local.properties`.
- [x] **Java Development Kit**: OpenJDK 21 (Android Studio JBR) detected and verified.
- [x] **Gradle**: Gradle 8.8 wrapper downloaded and cached.
- [x] **Project Configuration**: Gradle dependency resolution dry run verified (`BUILD SUCCESSFUL`).
- [ ] **APK Compilation**: Held (pending user instruction).
