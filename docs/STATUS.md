# Project status

Last updated: 2026-11-26

## Repository and branch state

- Public repository: https://github.com/siddumoni/fake-call-android
- Default branch: `main`; the initial README commit is the only project commit currently on main.
- PR 1: [Android project foundation and secure CI](https://github.com/siddumoni/fake-call-android/pull/1), head branch `initial-setup`, latest head at this update: `e067cd2797f938deec9c86815b90d58a9f59e109`.
- PR 2 working branch: `pr2-theme-system`, stacked on the current PR 1 head. Theme implementation is prepared separately so PR 1 does not absorb theme changes.
- No PR has been merged yet.

## Toolchain

- AGP 9.3.1 and Gradle 9.7.0 are within the Kotlin compatibility matrix's fully supported range for KGP 2.4.20–2.4.21.
- Kotlin / Compose compiler plugin: 2.4.21.
- Compose BOM 2026.09.00: latest stable BOM in Android's documented Compose setup that supports compileSdk 37.
- AndroidX Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0; ktlint 14.2.0.
- minSdk 34, compileSdk/targetSdk 37, JDK 17.
- Compose BOM 2026.11.01 was rejected because it selects Compose 1.13.0, whose AAR metadata requires compileSdk 37.1, exceeding the specified SDK 37.

## PR 1 — foundation

The initial foundation has a successful bootstrap run [#34](https://github.com/siddumoni/fake-call-android/actions/runs/45849001891) covering:
- Official Gradle 9.7.0 wrapper generation with distribution checksum and wrapper validation.
- Android SDK 37 platform and Build Tools installation.
- Dependency locks and SHA-256 verification metadata generated from clean-cache downloads.
- Debug and release APK builds, Android test-source compilation, JVM unit tests, Android Lint and ktlint.
- Strict dependency verification.
- Debug and release merged-manifest allowlist checks and central UI colour-token enforcement.

The earlier PR run [#45852868319](https://github.com/siddumoni/fake-call-android/actions/runs/45852868319) passed CodeQL, build/unit/lint/manifest and Gitleaks, but the emulator boot step remained in progress. It was superseded by later commits and is no longer current. The emulator boot workflow was hardened with bounded ADB startup and a job timeout.

The latest PR 1 run at this status update is [#45856126356](https://github.com/siddumoni/fake-call-android/actions/runs/45856126356), still in progress; do not treat pending checks as passing.

**External blocker:** Dependency Review fails because GitHub's Dependency Graph is disabled for this repository. The connected GitHub tools do not expose repository security-setting writes. Enable it in **Settings → Code security and analysis → Dependency graph → Enable**. Do not disable or bypass Dependency Review.

## PR 2 — theme system (work-in-progress branch)

The `pr2-theme-system` branch adds:
- DataStore persistence for Follow system, Light, Dark, Pure black (AMOLED) and Dynamic colour.
- Application-level night-mode setup before first Activity startup and matching day/night splash/window resources.
- Material 3 theme palettes and runtime text contrast fallback.
- Home and Appearance screens with accessible radio/switch controls and 48dp+ touch targets.
- Activity state restoration when night-mode changes recreate the Activity.
- JVM tests for setting defaults, storage mapping and palette contrast.
- Instrumented tests for DataStore persistence, interactions, and full-app screenshot captures for Home and Appearance in Light, Dark and AMOLED modes.

PR 2 has not yet been opened or run through CI. The branch has been synchronized with the current PR 1 base without mixing theme changes into PR 1.

## Remaining verification

- Current PR 1 checks must all complete and pass on the latest head SHA. Dependency Review currently needs the manual repository setting above.
- PR 2 must run the full same build, test, lint, emulator, screenshot, CodeQL, Gitleaks and dependency-review suite before it can be merged.
- No physical OnePlus Nord 6 / ColorOS 17 validation has been performed.
- No release signing secrets or signed release artifact exist.
- Repository ruleset, secret scanning and push protection still require owner-side configuration because the current connector does not expose repository-settings mutations.

## Next steps

1. Complete all PR 1 checks and merge only after Dependency Review is enabled and every required check passes on the current head.
2. Run PR 2's full CI suite and resolve all failures without weakening checks; retarget it to `main` after PR 1 merges.
3. Continue with presets/call screens, then scheduling/notifications/ringtone, then onboarding/settings/shortcuts/tile, and final hardening/release.
4. Apply the documented repository ruleset and security settings.
5. Complete the physical-device checklist on the OnePlus handset before claiming ColorOS-specific reliability.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain physical-handset checks.
