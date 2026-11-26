# Project status

Last updated: 2026-11-26

## Repository and branch state

- Public repository: https://github.com/siddumoni/fake-call-android
- Default branch: `main`; it contains only the initial README commit.
- PR 1: https://github.com/siddumoni/fake-call-android/pull/1, base `main`, head branch `initial-setup`.
- PR 2: https://github.com/siddumoni/fake-call-android/pull/2, base `initial-setup`, head branch `pr2-theme-system`.
- No PR has been merged and no direct pushes have been made to `main`.

## Toolchain

- AGP 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01, selecting stable Compose 1.13.0.
- Compose 1.13.0 requires compile API 37.1. The app uses the AGP minor API DSL to compile against API 37.1 while retaining targetSdk 37 and minSdk 34.
- SDK installer supports the API 37.1 compile platform and prefers matching Android 17 emulator images; it records the actual emulator API level.
- AndroidX Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0; ktlint Gradle plugin 14.2.0.

## PR 1 — foundation

A previous bootstrap run [#34](https://github.com/siddumoni/fake-call-android/actions/runs/45849001891) passed debug/release builds, JVM unit tests, Lint, ktlint, strict dependency verification, manifest allowlist checks and central UI colour-token checks using an earlier compatible Compose configuration.

The first validation after moving to Compose BOM 2026.11.01 correctly failed because the app lockfile still enforced BOM 2026.09.00. The dependency-refresh job has been updated to regenerate the lockfile from an unlocked app-lock snapshot and create new verification metadata from the actual resolved artifacts. That refresh is pending and its output is not yet verified.

The latest API 37.1 PR 1 run is visible at https://github.com/siddumoni/fake-call-android/actions. It has confirmed the SDK package can be installed, but the build and CodeQL jobs failed on the stale lockfile. The emulator test job was still running when these logs were inspected. Secret scan passed. Dependency Review fails because the repository Dependency Graph is disabled (`GITHUB_DEPENDENCY_GRAPH_ENABLED=false`).

**External blocker:** Enable Dependency Graph in repository **Settings → Code security and analysis → Dependency graph → Enable**. Do not disable or bypass Dependency Review.

## PR 2 — theme system

The `pr2-theme-system` branch adds:
- Preferences DataStore persistence for Follow system, Light, Dark, Pure black (AMOLED) and Dynamic colour.
- Application-level night-mode selection before first Activity startup and matching day/night splash/window resources.
- Material 3 light, dark and AMOLED palettes with runtime WCAG AA text-role fallback.
- Home and Appearance screens with accessible controls, screen-state restoration, edge-to-edge and predictive-back handling.
- JVM tests for defaults, storage mapping and palette contrast.
- Instrumented tests for DataStore persistence, theme interactions and Home/Appearance rendering in Light, Dark and AMOLED.
- A Pure Black palette that sets all Material 3 surface-container roles to pure black, with explicit assertions.

PR 2 is stacked on PR 1. Its build configuration and dependency BOM are being aligned to API 37.1 / Compose BOM 2026.11.01 so both branches use the same toolchain.

## Not yet verified

- Final PR checks have not all completed successfully on the latest head commits.
- API 37 emulator instrumentation and screenshot tests have not yet completed successfully on the latest head.
- Gradle dependency lock and verification metadata refresh is pending.
- Dependency Review needs the repository Dependency Graph setting enabled.
- Screenshot smoke tests capture images and validate dimensions; they are not yet pixel-perfect golden-image comparisons.
- No physical OnePlus Nord 6 / ColorOS 17 validation.
- No release signing secrets or signed release artifact.
- Ruleset, native secret scanning, push protection and Dependabot alerts require owner-side repository settings; the GitHub connector has no settings-write action.

## Next steps

1. Regenerate dependency locks and verification metadata using the current Compose BOM, then validate all PR 1 checks on its latest head.
2. Enable Dependency Graph and rerun Dependency Review.
3. Hostile-review and merge PR 1 only after every required check passes.
4. Rebase PR 2 onto main, run all checks on its latest head, then merge.
5. Continue with caller presets, incoming-call screens, scheduling/notifications/ringtones, onboarding/settings/shortcuts/tile, and final security hardening.
6. Apply repository rulesets, secret scanning, push protection and Dependabot alerts.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain handset tests documented in docs/DEVICE_TESTS.md.
