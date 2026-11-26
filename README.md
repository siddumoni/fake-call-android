# AlibiCall

AlibiCall is a private, offline Android application for scheduling a locally simulated incoming call on the device owner's phone.

It does not place real calls, spoof carrier caller ID, create call-log entries, or contact another person. It must not impersonate emergency services, authorities, banks, government bodies, or real companies.

## Platform and toolchain

- Application ID: `com.siddumoni.fakecall`
- Minimum SDK: 34
- Compile platform: Android 17 API 37.1 (minor API level 1)
- Target SDK: 37
- Android Gradle Plugin: 9.3.1
- Gradle: 9.7.0
- Kotlin Gradle Plugin and Compose compiler: 2.4.21
- Compose BOM: 2026.11.01 (stable)
- JDK: 17

The Compose 1.13.0 release selected by the November 2026 stable BOM requires API 37.1 at compile time. The Gradle Android DSL sets the API 37 minor level explicitly; the app still targets API 37 as requested. AGP 9.3.1 and Gradle 9.7.0 remain within the published fully-supported Kotlin Gradle Plugin compatibility range for Kotlin 2.4.20–2.4.21.

## Build

Use the checked-in Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. GitHub Actions installs the API 37.1 compile platform and selects the highest available Android 17 x86_64 emulator image (falling back to API 36 only when an Android 17 image is unavailable). The CI run summary records the actual emulator API level.

## Privacy and permissions

The foundation manifest intentionally has no permissions. Future features may add only reviewed permissions documented in [docs/SECURITY.md](docs/SECURITY.md), one feature at a time, with the merged manifest checked in CI.

No INTERNET, telephony, contacts, call log, SMS, microphone, camera, location, analytics, advertising, trackers, crash reporting, or remote configuration.

## Project documentation

- [Architecture and design](docs/DESIGN.md)
- [Security and privacy](docs/SECURITY.md)
- [Physical-device checklist](docs/DEVICE_TESTS.md)
- [Project status](docs/STATUS.md)
- [Release signing](docs/RELEASING.md)

Tag releases are signed when all four signing secrets are configured. Otherwise the workflow publishes a clearly labelled debug-variant APK (signed with the temporary Android debug key) and its SHA-256 checksum. See [docs/RELEASING.md](docs/RELEASING.md) for signing setup.
