# Project status

Last updated: 2026-11-27

## Repository and delivery state

- Repository: https://github.com/siddumoni/fake-call-android
- Foundation, appearance/theme system, caller profiles/local simulated-call preview, schedule domain/persistence foundation, and privacy-first first-run onboarding are merged into `main`.
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
- PR #19 is open and under CI review. It contains schedule create/edit/cancel/delete UI, exact-versus-approximate choice, contextual notification/exact-alarm permission flows, AlarmManager trigger/timeout adapters, non-exported receivers, a private heads-up notification, notification-to-local-simulation navigation and a Dismiss action. The branch includes rollback on alarm-registration failure, foreground alarm reconciliation, race-fenced pending deletion, UI/notification/scheduler tests, and the v1→v2 codec migration. It is not merged into `main`; CI for the latest PR head is still pending. Reboot restoration and physical-device validation remain outstanding.

## Current CI maintenance

- PR #11 fixed CodeQL extraction failing when Kotlin compilation was served from Gradle's build cache.
- PR #12 corrected this status document and was merged after its Android CI run succeeded.
- Dependency updates #3–#7 are merged. PR #8 (Gradle 9.9.2) and PR #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1 respectively. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated Kotlin/toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## Remaining product work

1. Merge PR #19 only after complete green CI, focused review, and verified race/rollback tests.
2. Add reboot recovery with a non-exported receiver, RECEIVE_BOOT_COMPLETED allowlist rationale, and valid-future-schedule reconciliation.
3. Cover notification denial/revocation, exact-alarm revocation/grant, duplicate delivery, cancellation and timeout with additional tests; document how pending schedules behave after permission loss.
4. Validate notification-to-simulation Answer/Decline/End and external-intent rejection; keep the notification heads-up fallback (no restricted full-screen intent).
5. Accessibility review, lifecycle/device tests, threat review, release checklist and production signing configuration (requires owner-provided secrets).

All feature changes should be focused PRs with tests and complete green CI. Keep the app local/offline, avoid real telephony or caller-ID spoofing, and never impersonate emergency services, authorities, banks, government bodies, or real companies.

## Physical-device validation limitation

A target OnePlus / ColorOS handset is not available in this environment. OEM alarm delivery, exact-alarm settings, lock-screen/full-screen notification behaviour, battery restrictions, audio routing and system-theme/splash behavior remain unverified on physical hardware. Do not mark these checks complete based on emulator CI alone.
