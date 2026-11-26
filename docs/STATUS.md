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
- SDK installer supports the API 37.1 compile platform and discovers available Android emulator images. If an Android 17 x86_64 image is not installable it falls back to API 36.
- AndroidX Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0; ktlint Gradle plugin 14.2.0.

## PR 1 — foundation and current validation

Latest observed PR 1 head: `0424e3302787070f09da1b46ccc0c47917105a81`.

Workflow run [45871868980](https://github.com/siddumoni/fake-call-android/actions/runs/45871868980) has completed these jobs successfully:
- Debug and release builds, JVM unit tests, Android Lint, ktlint and SDK installer tests.
- Exact merged-manifest permission policy and central UI colour-token checks.
- CodeQL analysis.
- Gitleaks secret scan.
- Debug APK artifact upload.

The API 37.1 emulator instrumentation job was still running at the time this status was written. It had launched the emulator with the documented TCG fallback because GitHub-hosted Linux runners do not expose KVM. A separate cancelled PR 2 attempt showed the device remaining `offline` during cold boot; that cancellation happened before instrumented tests completed, so it is not a test failure or a passing result.

Dependency Review fails because GitHub reports the repository Dependency Graph as unavailable/disabled. The workflow has not been weakened or bypassed. **Owner action required:** enable the Dependency Graph in repository Settings → Code security and analysis → Dependency graph, then rerun Dependency Review.

## PR 2 — theme system and current validation

Latest theme branch commit before this status update: `0159112385a269f60cfbe0caf38e8512df4ad713`.

Implemented:
- Preferences DataStore persistence for Follow system, Light, Dark, Pure black (AMOLED) and Dynamic colour.
- Application-level night-mode selection before first Activity startup and matching day/night splash/window resources.
- Material 3 light, dark and AMOLED palettes with runtime WCAG AA text-role fallback.
- Home and Appearance screens with accessible controls, screen-state restoration, edge-to-edge and predictive-back handling.
- JVM tests for defaults, storage mapping and palette contrast.
- Instrumented tests for DataStore persistence, theme interactions and Home/Appearance rendering in Light, Dark and AMOLED.
- A Pure Black palette that sets all Material 3 surface-container roles to pure black, with explicit assertions.

The screenshot smoke suite captures both Home and Appearance in Light, Dark and Pure Black. It checks render dimensions and exports PNGs; it is not a pixel-perfect golden-image comparison.

A previous clean-cache build on the theme branch failed first on screenshot-test ktlint formatting; that was corrected. A subsequent attempt on the older theme commit failed during resolution of the Compose compiler plugin marker. The same Kotlin 2.4.21 plugin coordinates are published by the Kotlin Team and were resolved successfully by the PR 1 build, so this appears to be a transient dependency-repository resolution failure, not a reason to downgrade the Kotlin version. The latest head needs a fresh full CI run after this status/documentation commit.

Dependency Review is skipped for push-triggered runs and remains blocked on PR 1 until the repository Dependency Graph is enabled.

## Not yet verified

- A complete connected Android instrumentation run, including successful cold boot and all six screenshot exports, on the latest PR heads.
- Dependency Review on PR 1 after the repository Dependency Graph is enabled.
- Physical OnePlus Nord 6 / ColorOS 17 validation.
- Release signing secrets or a signed production release artifact.
- Repository rulesets, native push protection and Dependabot alert settings. No repository-settings write action is available through the connected GitHub integration.

## Next steps

1. Complete the PR 1 API 37 emulator instrumentation and screenshot run.
2. Enable Dependency Graph and rerun PR 1 Dependency Review.
3. Run the full CI matrix against the latest PR 2 head, correcting any remaining failures without weakening checks.
4. Hostile-review and merge PR 1 only after all required checks pass.
5. Retarget/rebase PR 2 onto `main`, then validate and merge it.
6. Continue with caller presets, incoming-call screens, scheduling/notifications, ringtone design, onboarding/settings/shortcuts/tile and final security hardening.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain handset tests documented in docs/DEVICE_TESTS.md.
