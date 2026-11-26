# AlibiCall

AlibiCall is a private, offline Android application for scheduling a locally simulated incoming call on the device owner's phone.

It does not place real calls, spoof carrier caller ID, create call-log entries, or contact another person. It must not impersonate emergency services, authorities, banks, government bodies, or real companies.

## Platform and toolchain

- Application ID: `com.siddumoni.fakecall`
- Minimum SDK: 34
- Compile / target SDK: 37
- Kotlin Gradle Plugin and Compose compiler: 2.4.21
- Android Gradle Plugin: 9.3.1
- Gradle: 9.7.0
- Compose BOM: 2026.11.01
- JDK: 17

AGP 9.3.1 and Gradle 9.7.0 are deliberately paired with Kotlin 2.4.21 because the Kotlin team's published compatibility matrix lists 9.3.1 and 9.7.0 as the upper fully supported AGP and Gradle versions for KGP 2.4.20–2.4.21. Newer AGP/Gradle releases exist, but they are outside that fully supported matrix. AGP 9.3 supports API 37. The Compose 2026.11.01 BOM is the latest stable BOM documented by Android and maps the stable Compose UI/Foundation 1.13.0 and Material3 1.5.0 line.

## Build

Use the checked-in Gradle Wrapper:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat`. GitHub Actions installs Android SDK 37 and runs screenshot smoke tests on the highest available x86_64 Android emulator image, recording the selected API level in the run summary.

## Privacy and permissions

The foundation manifest intentionally has no permissions. Future features may add only reviewed permissions documented in [docs/SECURITY.md](docs/SECURITY.md), one feature at a time, with the merged manifest checked in CI.

No INTERNET, telephony, contacts, call log, SMS, microphone, camera, location, analytics, advertising, trackers, crash reporting, or remote configuration.

## Project documentation

- [Architecture and design](docs/DESIGN.md)
- [Security and privacy](docs/SECURITY.md)
- [Physical-device checklist](docs/DEVICE_TESTS.md)
- [Project status](docs/STATUS.md)
- [Release signing](docs/RELEASING.md)

Release builds are signed only when all four signing secrets are configured. Until then, the tag workflow publishes a clearly labelled unsigned debug APK and SHA-256 checksum.
