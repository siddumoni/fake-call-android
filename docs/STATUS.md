# Project status

Last updated: 2026-11-26

## Repository and branch state

- Public repository: https://github.com/siddumoni/fake-call-android
- Default branch: `main`; it contains only the initial README commit.
- PR 1: https://github.com/siddumoni/fake-call-android/pull/1, head branch `initial-setup`, base `main`.
- PR 2: https://github.com/siddumoni/fake-call-android/pull/2, head branch `pr2-theme-system`, stacked on PR 1.
- No PR has been merged and no direct pushes have been made to `main`.
- The Gradle 9.7.0 wrapper includes the official distribution SHA-256 and passed wrapper validation.
- Gradle dependency lockfiles and verification metadata were refreshed successfully for Compose BOM 2026.11.01 in workflow run [45863446695](https://github.com/siddumoni/fake-call-android/actions/runs/45863446695).
- Normal build workflows use read-only repository contents permissions. The release workflow grants contents write only to the publish job.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01, selecting stable Compose 1.13.0.
- Compose 1.13.0 requires compile API 37.1. The app uses the AGP minor API DSL to compile against API 37.1 while retaining targetSdk 37 and minSdk 34.
- The SDK installer discovers the API 37.1 compile platform and Build Tools. It prefers the highest installable Android 17 emulator image and falls back to API 36 only when no Android 17 x86_64 image can be installed.
- AndroidX Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0; ktlint Gradle plugin 14.2.0 (KtLint 1.5.0 default).

## PR 1 — foundation and latest CI evidence

Latest validated PR 1 head: `c0ff370e72109039fa6dc109e70e91ca9056abe4`.

Workflow run [45874209298](https://github.com/siddumoni/fake-call-android/actions/runs/45874209298) completed these jobs successfully:
- Debug and release APK builds.
- JVM unit tests, Android Lint and ktlint.
- SDK installer unit tests.
- Exact debug/release merged-manifest permission allowlist and UI colour-token checks.
- CodeQL analysis.
- Gitleaks secret scan.
- Android 17 API 37 emulator boot and `:app:connectedDebugAndroidTest`.

The emulator job confirmed `/dev/kvm` was readable and writable, so it used hardware acceleration rather than TCG fallback. Its connected Android test command finished with `BUILD SUCCESSFUL`.

**Only remaining PR 1 CI failure:** Dependency Review fails because GitHub reports that this repository's Dependency Graph is disabled/unavailable. The workflow has not been weakened or bypassed. **Owner action required:** enable Dependency Graph in repository Settings → Code security and analysis → Dependency graph, then rerun Dependency Review.

The debug APK artifact was uploaded by this successful build job. The repository still has no release signing secrets or signed production release.

## PR 2 — theme system

The `pr2-theme-system` branch adds:
- Preferences DataStore persistence for Follow system, Light, Dark, Pure black (AMOLED) and Dynamic colour.
- Application-level night-mode selection before first Activity startup and matching day/night splash/window resources.
- Material 3 light, dark and AMOLED palettes with runtime WCAG AA text-role fallback.
- Home and Appearance screens with accessible controls, screen-state restoration, edge-to-edge and predictive-back handling.
- JVM tests for defaults, storage mapping and palette contrast.
- Instrumented tests for DataStore persistence, theme interactions and Home/Appearance rendering in Light, Dark and AMOLED.
- A Pure Black palette that sets all Material 3 surface-container roles to pure black, with explicit assertions.

A clean-cache PR 2 build exposed ktlint violations in the newly added theme implementation and its tests. A one-time KtLint 1.5.0 format-and-check workflow is running against the current theme branch; its formatted source commit must receive a fresh full CI run before PR 2 can be considered validated.

The screenshot smoke suite captures Home and Appearance in Light, Dark and Pure Black. It checks render dimensions and exports PNGs; it is not a pixel-perfect golden-image comparison.

## Not yet verified

- A full green PR 2 CI run on the latest formatted theme branch.
- Dependency Review on PR 1 after the repository Dependency Graph is enabled.
- Physical OnePlus Nord 6 / ColorOS 17 validation.
- Release signing secrets or a signed production release artifact.
- Repository rulesets, native push protection and Dependabot alert settings. No repository-settings write action is available through the connected GitHub integration.

## Next steps

1. Enable Dependency Graph and rerun PR 1 Dependency Review.
2. Complete ktlint formatting and the full PR 2 CI matrix.
3. Hostile-review and merge PR 1 only after Dependency Review and all other required checks pass.
4. Retarget/rebase PR 2 onto `main`, then validate and merge it.
5. Continue with caller presets, incoming-call screens, scheduling/notifications, ringtone design, onboarding/settings/shortcuts/tile and final security hardening.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain handset tests documented in docs/DEVICE_TESTS.md.
