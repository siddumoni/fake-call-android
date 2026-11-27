# Project status

Last updated: 2026-11-27

## Repository and delivery state

- Repository: https://github.com/siddumoni/fake-call-android
- Foundation, appearance/theme system, and caller profiles/local simulated-call preview are merged into `main`.
- CI workflows are SHA-pinned, dependency verification is enabled, and the merged-manifest permission policy is enforced.
- CI builds a debug APK and validates JVM, lint, instrumentation, screenshot, CodeQL, secret-scan and dependency-review jobs.
- Production signing is not configured; a production-signed release cannot yet be produced.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 34.
- CI uses an Android 17 emulator when the image is available, with KVM acceleration where supported.

## Merged implementation

### Foundation and secure CI

- Debug/release variants, wrapper validation and strict dependency verification.
- Build, unit tests, lint/ktlint, merged-manifest permission policy, CodeQL and Gitleaks.
- Android emulator instrumentation and screenshot collection.
- Offline-only/privacy-first manifest and security documentation.

### Appearance system

- Preferences DataStore-backed Follow system / Light / Dark modes.
- Independent AMOLED pure-black and Dynamic colour toggles.
- Material 3 palettes, contrast fallback, edge-to-edge and startup theme application.
- Accessible controls, state restoration, JVM and instrumentation coverage, screenshots.

### Caller profiles and local simulated-call preview

- Local caller-profile create/edit/delete and preview.
- Fictional display names and optional number labels with validation.
- App-private Preferences DataStore storage.
- Incoming/answered simulation screens with explicit simulation disclaimer.
- No actual telephony, call-log, network or remote communication.

## Current CI maintenance

- PR #11 fixed CodeQL extraction failing when Kotlin compilation was served from Gradle's build cache.
- PR #12 corrected this status document and was merged after its Android CI run succeeded.
- Dependency updates #3–#7 are merged. PR #8 (Gradle 9.9.2) and PR #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1 respectively. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated Kotlin/toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## Remaining product work

1. Scheduling UX and a robust schedule state machine (validation, timezone/DST handling, persistence, cancellation and idempotency).
2. Exact-alarm permission handling with graceful inexact fallback and permission-revocation recovery.
3. Incoming-call notification, lock-screen/full-screen behavior when permitted, and a clear heads-up fallback when not.
4. Reboot recovery for pending schedules; restore only valid future alarms and avoid duplicate deliveries.
5. Ringtone/vibration controls with safe audio lifecycle, focus and routing behaviour.
6. Onboarding, accessibility, empty/error states and resilient storage handling.
7. Additional automated tests, threat review, release checklist and signing configuration (requires owner-provided secrets).

All feature changes should be focused PRs with tests and complete green CI. Keep the app local/offline, avoid real telephony or caller-ID spoofing, and never impersonate emergency services, authorities, banks, government bodies, or real companies.

## Physical-device validation limitation

A target OnePlus / ColorOS handset is not available in this environment. OEM alarm delivery, exact-alarm settings, lock-screen/full-screen notification behaviour, battery restrictions, audio routing and system-theme/splash behavior remain unverified on physical hardware. Do not mark these checks complete based on emulator CI alone.
