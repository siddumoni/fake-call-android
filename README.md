# AlibiCall

AlibiCall is a private, offline Android application being built to schedule a locally simulated incoming call on the device owner's phone.

It does not place real calls, spoof carrier caller ID, create call-log entries, or contact another person. It must not impersonate emergency services, authorities, banks, government bodies, or real companies.

## Platform and toolchain

- Application ID: `com.siddumoni.fakecall`
- Minimum SDK: 35 (Android 15)
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
- Local caller-profile CRUD (fictional display name and optional number label), persisted in app-private Preferences DataStore.
- Preview incoming and answered simulated-call screens with an explicit simulation banner; no telephony or network integration.
- Scheduled-call domain model with UUID/time-horizon validation, deterministic DST gap/overlap resolution, versioned persistence codec, app-private DataStore repository, atomic action-based transitions and emulator repository tests.
- First-run privacy onboarding that explains the offline-only, fictional-profile and no-real-calls boundaries. Completion is stored as a single boolean in app-private Preferences DataStore.
- Schedule create/edit/cancel/delete UI, exact and approximate alarms, contextual permission handling, notifications, ring timeout and notification actions.
- Restart, app-update and exact-alarm-grant recovery through a non-exported receiver. A restart restores only future schedules and closes expired or interrupted records.

Ringtone and vibration controls are not implemented yet. Physical-device delivery is unverified.

## Build

Use the checked-in Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. Main-branch CI compiles against Android API 37.1 and runs instrumented tests on an Android 17 emulator. PR #23 adds emulator coverage for Android 15 (API 35), Android 16 (API 36) and Android 17 (API 37), using compact (360 dp), standard (~411 dp), and expanded (600 dp) display configurations plus 200% font scaling on compact configurations. This matrix is not considered complete until PR #23 is merged and every matrix job passes. Before emulator startup, CI attempts to make `/dev/kvm` accessible. The runner uses KVM hardware acceleration where available and selects Android Emulator TCG software emulation otherwise; the boot timeout is 15 minutes.

## Privacy and permissions

The application source manifest declares no feature permissions. AndroidX Core currently contributes one package-specific, signature-protected compatibility permission to each merged APK manifest. CI allows only the exact debug/release application-ID variants and verifies that the declared protection level remains `signature`; see [docs/SECURITY.md](docs/SECURITY.md). This dependency permission is unrelated to telephony or access to user data.

No INTERNET, CALL_PHONE, READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS, SEND_SMS, microphone, camera, location, analytics, advertising, tracking, crash reporting or remote configuration permissions are approved.

## Project documentation

- [Architecture and design](docs/DESIGN.md)
- [Security and privacy](docs/SECURITY.md)
- [Physical-device checklist](docs/DEVICE_TESTS.md)
- [Project status](docs/STATUS.md)
- [Release signing](docs/RELEASING.md)

Tag releases are signed when all four signing secrets are configured. Otherwise the workflow publishes a clearly labelled debug-variant APK (signed with the temporary Android debug key) and its SHA-256 checksum. See [docs/RELEASING.md](docs/RELEASING.md) for signing setup.
