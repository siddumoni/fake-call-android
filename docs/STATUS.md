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

- AGP 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01, selecting stable Compose 1.13.0.
- Compose 1.13.0 requires compile API 37.1. The app uses the AGP minor API DSL to compile against API 37.1 while retaining targetSdk 37 and minSdk 34.
- SDK installer supports the API 37.1 compile platform and discovers available Android emulator images. If an Android 17 x86_64 image is not installable it falls back to API 36.
- AndroidX Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0; ktlint Gradle plugin 14.2.0, whose current configured default KtLint engine is 1.5.0.

## PR 1 — foundation and latest CI evidence

The latest PR 1 code-validation commit was `c0ff370e72109039fa6dc109e70e91ca9056abe4`. Workflow run [45874209298](https://github.com/siddumoni/fake-call-android/actions/runs/45874209298) passed:
- Debug and release APK builds.
- JVM unit tests, Android Lint and ktlint.
- SDK installer unit tests.
- Exact merged-manifest permission allowlist and central UI colour-token checks.
- CodeQL analysis and Gitleaks secret scan.
- Android 17 API 37 emulator boot and `:app:connectedDebugAndroidTest`.

The emulator job confirmed `/dev/kvm` was readable and writable. It therefore used hardware acceleration rather than TCG fallback, booted API 37, and finished connected Android tests with `BUILD SUCCESSFUL`.

The PR 1 documentation was then updated in commit `c72553b70d111003a83ee0ba835d9a2e6c0db283`. A fresh CI run [45875676281](https://github.com/siddumoni/fake-call-android/actions/runs/45875676281) is validating that current documentation head.

**Only remaining PR 1 CI failure:** Dependency Review fails because GitHub reports that this repository's Dependency Graph is disabled/unavailable. The workflow has not been weakened or bypassed. **Owner action required:** enable Dependency Graph in repository Settings → Code security and analysis → Dependency graph, then rerun Dependency Review.

The debug APK artifact was uploaded by the successful build job. The repository still has no release signing secrets or signed production release.

## PR 2 — theme system and latest code state

Latest theme implementation commit after automatic formatting: `0ef378779dd292bf6924f7825c8726742ccd2439`.

Implemented:
- Preferences DataStore persistence for Follow system, Light, Dark, Pure black (AMOLED) and Dynamic colour.
- Application-level night-mode selection before first Activity startup and matching day/night splash/window resources.
- Material 3 light, dark and AMOLED palettes with runtime WCAG AA text-role fallback.
- Home and Appearance screens with accessible controls, screen-state restoration, edge-to-edge and predictive-back handling.
- JVM tests for defaults, storage mapping and palette contrast.
- Instrumented tests for DataStore persistence, theme interactions and Home/Appearance rendering in Light, Dark and AMOLED.
- A Pure Black palette that sets all Material 3 surface-container roles to pure black, with explicit assertions.

A clean-cache PR 2 build identified KtLint 1.5.0 formatting violations across the new theme implementation and tests. One-time formatting workflow [45875349572](https://github.com/siddumoni/fake-call-android/actions/runs/45875349572) completed successfully: `:app:ktlintFormat` ran, followed by `:app:ktlintCheck`, and the workflow committed the formatted source files and removed itself. A fresh full CI run against commit `0ef3787` is still required before PR 2 can be considered validated.

The screenshot smoke suite captures Home and Appearance in Light, Dark and Pure Black. It checks render dimensions and exports PNGs; it is not a pixel-perfect golden-image comparison.

The Follow system mode uses application-level night mode AUTO, which maps to an undefined per-app night-mode override in the Android framework so the system configuration flows through. Light and Dark modes explicitly set the app override.

## Not yet verified

- A complete connected Android instrumentation run, including all six screenshot exports, on the latest formatted PR 2 head.
- Dependency Review on PR 1 after the repository Dependency Graph is enabled.
- Physical OnePlus Nord 6 / ColorOS 17 validation.
- Release signing secrets or a signed production release artifact.
- Repository rulesets, native push protection and Dependabot alert settings. No repository-settings write action is available through the connected GitHub integration.

## Next steps

1. Enable Dependency Graph and rerun PR 1 Dependency Review.
2. Complete the full PR 2 CI matrix on the formatted theme branch.
3. Hostile-review and merge PR 1 only after Dependency Review and all other required checks pass.
4. Retarget/rebase PR 2 onto `main`, then validate and merge it.
5. Continue with caller presets, incoming-call screens, scheduling/notifications, ringtone design, onboarding/settings/shortcuts/tile and final security hardening.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain handset tests documented in docs/DEVICE_TESTS.md.
