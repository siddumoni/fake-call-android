# AlibiCall

AlibiCall is a private, offline Android application being built to schedule a locally simulated incoming call on the device owner's phone.

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

On Windows use `gradlew.bat`. GitHub Actions installs Android API 37.1 and selects the highest installable Android 17 x86_64 emulator image, falling back to API 36 only when an Android 17 image cannot be installed. On Linux runners CI enables KVM access where available and uses Android Emulator TCG software emulation otherwise. The selected emulator API is recorded in the workflow summary.

## Privacy and permissions

The application source manifest declares no feature permissions. AndroidX Core contributes one package-specific, signature-protected compatibility permission and matching permission request to each merged APK manifest. CI allows only the exact debug/release application-ID variants and verifies that the protection level remains `signature`; see [docs/SECURITY.md](docs/SECURITY.md). This dependency permission is not related to telephony, contacts, call history, SMS, microphone, camera, storage or network access.

No INTERNET, CALL_PHONE, READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS, SEND_SMS, RECORD_AUDIO, CAMERA, location, analytics, advertising, tracking, crash reporting or remote configuration permissions are approved.

## Project documentation

- [Architecture and design](docs/DESIGN.md)
- [Security and privacy](docs/SECURITY.md)
- [Physical-device checklist](docs/DEVICE_TESTS.md)
- [Project status](docs/STATUS.md)
- [Release signing](docs/RELEASING.md)

Tag releases are signed when all four signing secrets are configured. Otherwise the workflow publishes a clearly labelled debug-variant APK (signed with the temporary Android debug key) and its SHA-256 checksum. See [docs/RELEASING.md](docs/RELEASING.md) for signing setup.
