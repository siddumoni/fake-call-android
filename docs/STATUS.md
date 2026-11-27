# Project status

Last updated: 2026-11-27

## Repository and delivery state

- Repository: https://github.com/siddumoni/fake-call-android
- PR 1 (foundation and secure CI) is merged into `main`.
- PR 2 (persistent appearance / theme) is open and being validated.
- PR 3 (caller profiles and local simulated-call preview) is in development on a branch stacked on PR 2.
- Features are delivered in focused PRs; no direct feature pushes to `main`.
- CI action references are pinned to commit SHAs. Dependency lockfiles and verification metadata are committed.
- Release signing is not configured yet. CI can build a debug APK, but a production-signed release is not available.

## Toolchain

- AGP 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 34.
- Emulator CI uses Android 17 API 37 when available and KVM hardware acceleration on hosted runners.

## PR 1 — foundation (merged)

Implemented:
- Secure project foundation, release/debug variants and wrapper validation.
- CI builds, unit tests, lint/ktlint, merged-manifest permission policy, CodeQL and Gitleaks.
- Android emulator instrumentation.
- Offline-only/privacy-first app manifest and security documentation.

The foundation PR was merged after the successful Android build/test and security validations. The repository Dependency Graph setting is still not confirmed enabled, so OSV dependency review coverage depends on the repository setting.

## PR 2 — appearance system (open)

Implemented:
- Preferences DataStore-backed Follow system / Light / Dark appearance.
- Independent Pure black (AMOLED) and Dynamic colour toggles.
- Material 3 palettes and WCAG AA contrast fallback.
- Startup night-mode selection, matching day/night splash resources and edge-to-edge UI.
- Accessible appearance controls and screen-state restoration.
- JVM and instrumentation tests plus six screenshots across Home / Appearance and Light / Dark / AMOLED.

Latest CI fixes:
- Screenshot package discovery now queries packages by the AlibiCall package prefix instead of relying on the third-party package filter.
- The screenshot test's ktlint multiline-expression violation is fixed.
- A CI run is validating the current PR 2 head. The latest prior run had successful connected Android tests (3/3) but failed screenshot export because package discovery returned no candidates and ktlint flagged one expression. The failure is being corrected; a fresh all-green run is required before merge.

## PR 3 — caller profiles and local preview (in development)

Implemented on the stacked feature branch:
- Local caller-profile CRUD: create, edit, delete and preview.
- Fictional display name and optional number label; strict character/length validation.
- App-private Preferences DataStore storage with UUID records and URL-safe Base64 field encoding.
- Explicit simulated-call banner; preview can be answered or ended locally.
- No phone/network call, telephony, call-log, notification, scheduling or ringtone functionality in this slice.
- Unit tests for validation/codec and Compose instrumentation tests for caller-profile creation and call preview.
- DataStore instrumentation test for create/update/sort/delete.

## Remaining delivery steps

1. Get PR 2's current CI run fully green and merge it.
2. Rebase/retarget PR 3 onto `main`, validate it, then merge.
3. Implement scheduled-call state machine and exact-alarm permission handling.
4. Implement incoming-call notification/full-screen fallback with permission-health UI.
5. Validate audio/ringtone behaviour under Android 17 hardening and target ColorOS handset.
6. Add boot recovery, cancellation/idempotency, onboarding, accessibility and final security checks.
7. Configure release signing only when secrets are provided by the repository owner.

## Device validation limitation

The target OnePlus / ColorOS device is not available in this environment. OEM alarm delivery, lock-screen and full-screen intent behaviour, battery restrictions, audio routing and system-theme/splash behaviour must be verified physically. Do not mark device checklist items complete based only on emulator CI.
