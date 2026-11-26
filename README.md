# AlibiCall

AlibiCall is a private, offline Android application being built to schedule a locally simulated incoming call on the device owner's phone.

It does not place real calls, spoof carrier caller ID, create call-log entries, or contact another person. It must not impersonate emergency services, authorities, banks, government bodies, or real companies.

## Platform and toolchain

- Application ID: `com.siddumoni.fakecall`
- Minimum SDK: 34
- Compile platform: Android 17 API 37.1 (minor API level 1)
- Target SDK: 37
- Kotlin Gradle Plugin and Compose compiler: 2.4.21
- Android Gradle Plugin: 9.3.1
- Gradle: 9.7.0
- Compose BOM: 2026.11.01 (stable)
- JDK: 17

AGP 9.3.1 and Gradle 9.7.0 are within the published fully-supported Kotlin Gradle Plugin compatibility range for Kotlin 2.4.20–2.4.21. The November 2026 stable Compose BOM selects Compose 1.13.0, which requires compile API 37.1. The project compiles against API 37.1 through AGP's minor API DSL while retaining targetSdk 37.

## Implemented in the current development branch

- Preferences DataStore-backed appearance settings.
- Follow system, Light and Dark appearance options.
- Independent Pure black (AMOLED) and Dynamic colour toggles.
- Central Material 3 light, dark and AMOLED palettes with WCAG AA text contrast checks.
- Application-level night mode selection before the first Activity, edge-to-edge surfaces and saved screen state through mode changes.
- Screenshot, interaction, preference and colour-contrast tests for the theme slice.

The simulated-call feature set is being delivered in subsequent focused pull requests. No call scheduling, incoming-call UI or ringtone functionality is claimed complete yet.

## Build

Use the checked-in Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. GitHub Actions installs Android API 37.1 and tests on the highest available Android 17 x86_64 emulator image, recording the selected API level in the run summary.

## Privacy and permissions

The foundation manifest intentionally has no active Android permissions. Future features may add only reviewed permissions documented in [docs/SECURITY.md](docs/SECURITY.md), one feature at a time, with the merged manifest checked in CI.

No INTERNET, telephony, contacts, call log, SMS, microphone, camera, location, analytics, advertising, trackers, crash reporting or remote configuration.

## Project documentation

- [Architecture and design](docs/DESIGN.md)
- [Security and privacy](docs/SECURITY.md)
- [Physical-device checklist](docs/DEVICE_TESTS.md)
- [Project status](docs/STATUS.md)
- [Release signing](docs/RELEASING.md)

Tag releases are signed when all four signing secrets are configured. Otherwise the workflow publishes a clearly labelled debug-variant APK (signed with the temporary Android debug key) and its SHA-256 checksum. See [docs/RELEASING.md](docs/RELEASING.md) for signing setup.
