# Project status

Last updated: 2026-11-27

## Repository and delivery state

- Repository: https://github.com/siddumoni/fake-call-android
- Foundation, appearance/theme system, caller profiles/local simulated-call preview, and the schedule domain/persistence foundation are merged into `main`.
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

### Schedule domain foundation (PR #16)

- Pure Kotlin scheduled-call model with explicit state/action transitions: SCHEDULED → RINGING → IN_CALL → ENDED, with CANCELLED and TIMED_OUT terminal states.
- Validates canonical UUID identifiers, future trigger timestamps and a maximum one-year horizon with overflow-safe arithmetic.
- Resolves daylight-saving gaps to the first valid local time after the gap and overlaps deterministically to the earlier offset.
- Versioned delimiter-safe codec rejects malformed records and malformed identifiers.
- App-private Preferences DataStore repository supports sorted observation, creation/editing of pending records, deletion, terminal cleanup and atomic action-based lifecycle transitions.
- Concurrent duplicate alarm delivery and illegal transitions are rejected by serialized DataStore edits.
- JVM model/codec/time-rule tests and emulator repository instrumentation tests cover DST, validation, persistence, concurrent duplicate delivery and illegal transitions.
- PR #16 merged at `b847454dc7bd12e86473e706866196f549c098d2`; its complete PR CI passed, including build/unit/lint/manifest, emulator instrumentation/screenshots, CodeQL, secret scan and OSV dependency review.
- AlarmManager integration, schedule CRUD UI, permission checks, notifications and reboot recovery are not yet implemented.

## Current CI maintenance

- PR #11 fixed CodeQL extraction failing when Kotlin compilation was served from Gradle's build cache.
- PR #12 corrected this status document and was merged after its Android CI run succeeded.
- Dependency updates #3–#7 are merged. PR #8 (Gradle 9.9.2) and PR #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1 respectively. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated Kotlin/toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## Remaining product work

1. Scheduling create/edit/delete UI and integration with the merged schedule domain; display exact/approximate delivery status and reject stale caller-profile references.
2. Exact-alarm permission handling with graceful inexact fallback and permission-revocation recovery.
3. Incoming-call notification, lock-screen/full-screen behavior when permitted, and a clear heads-up fallback when not.
4. Reboot recovery for pending schedules; restore only valid future alarms and avoid duplicate deliveries.
5. Ringtone/vibration controls with safe audio lifecycle, focus and routing behaviour.
6. Finish first-run privacy onboarding, accessibility, empty/error states and resilient storage handling. Onboarding code and docs are in the current development branch and are not merged yet.
7. Additional automated tests, threat review, release checklist and signing configuration (requires owner-provided secrets).

All feature changes should be focused PRs with tests and complete green CI. Keep the app local/offline, avoid real telephony or caller-ID spoofing, and never impersonate emergency services, authorities, banks, government bodies, or real companies.

## Physical-device validation limitation

A target OnePlus / ColorOS handset is not available in this environment. OEM alarm delivery, exact-alarm settings, lock-screen/full-screen notification behaviour, battery restrictions, audio routing and system-theme/splash behavior remain unverified on physical hardware. Do not mark these checks complete based on emulator CI alone.
