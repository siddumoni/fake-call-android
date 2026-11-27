# Project status

Last updated: 2026-11-27

## Repository state

- Repository: https://github.com/siddumoni/fake-call-android
- Latest verified main merge: PR #22 at `b054770e90b515eae65c638e57a9633cbf9fbb2d`.
- Minimum OS: Android 15 (API 35); target SDK 37; compile platform Android 17 API 37.1.
- PRs #19–#22 are merged. Their PR-triggered Android CI runs completed successfully across all required jobs.
- No open pull requests or issues were returned by the repository search during this audit.
- The main-branch CI currently tests an Android 17 emulator. A six-entry Android 15/16/17 and display/font-scale compatibility matrix is proposed in the open maintenance branch `ci/android15-responsive-emulator-matrix`; it is not yet merged or CI-validated.
- Production signing secrets are not configured.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 35.
- Current CI uses an Android 17 emulator. The pending compatibility-matrix change adds API 35 and 36 test images and compact/standard/expanded display configurations.

## Merged implementation

### Foundation and secure CI

- Debug/release variants, wrapper validation and strict dependency verification.
- Build, unit tests, lint/ktlint, merged-manifest permission policy, CodeQL, Gitleaks and OSV dependency review.
- Android emulator instrumentation and theme screenshots.
- Offline-only/privacy-first manifest and security documentation.

### Appearance and adaptive layout improvements

- Preferences DataStore-backed Follow system / Light / Dark modes.
- Independent AMOLED pure-black and Dynamic colour toggles.
- Material 3 palettes, contrast fallback, edge-to-edge and startup theme application.
- Theme interaction, contrast, state restoration and screenshot coverage.
- Caller and schedule editors retain input across rotation/theme changes; caller editor scrolls.
- Android 15 minimum SDK and device-independent documentation are merged in PR #21.
- Broad physical OEM/display coverage remains unverified.

### Caller profiles and local simulated-call preview

- Local caller-profile create/edit/delete and preview.
- Fictional display names and optional number labels with validation.
- App-private Preferences DataStore storage.
- Incoming/answered simulation screens with explicit simulation disclaimer.
- No actual telephony, call-log, network or remote communication.

### Schedule lifecycle

- Pure Kotlin schedule model, UUID/time-horizon validation, deterministic DST handling, versioned codec and app-private DataStore repository.
- Create/edit/cancel/delete UI; exact and approximate delivery selection.
- Explicit exact-alarm permission flow; no silent downgrade.
- AlarmManager trigger/timeout adapters, unique immutable PendingIntents and non-exported receivers.
- High-importance private heads-up notification with generic public version, Dismiss action and strict app URI navigation; no full-screen-intent permission.
- Save rollback, cancellation fencing, duplicate-save guard, notification URI validation and lifecycle reconciliation.
- PR #19 merged at `28d2ded7745d3f80c3b0cee25d4c0ed4c5d4c834`.

### Recovery

- Non-exported restore receiver for boot, package replacement and exact-alarm access grants; valid future alarms are re-registered and expired/interrupted records closed.
- Manual clock and timezone changes reconcile alarms. Future absolute instants are preserved; overdue schedules inside grace windows are re-armed once, expired schedules are closed.
- PR #20 merged at `4dd642a92a2f9606e4281c3fd165bb89b0558043`.
- PR #22 merged at `b054770e90b515eae65c638e57a9633cbf9fbb2d`.

## Verified PR CI

| PR | Merge SHA | CI run | Result |
|---|---|---|---|
| #19 Schedule UI, alarms and notifications | `28d2ded` | 45987430593 | All jobs green |
| #20 Restart/package-update recovery | `4dd642a` | 45988349158 | All jobs green |
| #21 Android 15 minimum + layout fixes | `1b533590` | 45989170051 | All jobs green |
| #22 Clock/timezone reconciliation | `b054770` | 45990412713 | All jobs green |

Each listed run completed build/unit/lint/manifest policy, emulator instrumentation/screenshots, CodeQL, secret scan and OSV dependency review successfully. The OSV reusable workflow's optional code-scanning export steps are skipped by configuration; the scanner job itself succeeded.

The separate post-merge push run on main cannot be independently confirmed through the available GitHub connector because its workflow-run lookup only returns PR-triggered runs and combined-status results were empty.

## Remaining actionable work

1. Merge the Android 15/16/17 display/font-scale emulator matrix only after its Python tests, workflow validation and all six emulator jobs are green.
2. Expand lifecycle tests for notification permission denial/revocation, exact-alarm revocation, notification action paths, cancellation races, storage errors and restore edge cases.
3. Perform an accessibility review across every screen: TalkBack traversal, semantics, 48 dp targets, 200% fonts, contrast and reduced-motion behavior.
4. Ringtone/vibration controls and custom audio handling are not implemented. Decide whether they are in scope; if implemented, review permissions/audio focus and test interruption/routing safely.
5. Complete manual security/threat review and verify release minification, artifact, signature and checksum.
6. Configure production signing using owner-provided secrets; no keystore or password may be created or committed by automation.
7. Run physical-device tests on multiple OEMs, including OnePlus/ColorOS, Samsung and at least one Pixel or AOSP device, when hardware is available.
8. Compare stale branches with main and delete only branches proven obsolete. Branch deletion is not exposed by the current GitHub connector.

## Dependency maintenance

Dependabot PRs #3–#7 are merged. PRs #8 (Gradle 9.9.2) and #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## External validation blockers

- No physical Android device is connected in this environment. Alarm reliability under OEM battery restrictions, lock-screen presentation, reboot, timezone and clock changes, audio routing, font scaling and manufacturer-specific settings remain unverified. Emulator tests must not be reported as physical-device tests.
- Production signing requires owner-provided keystore and GitHub Actions secrets.
- The automated code-review service reported account usage limits for PRs #19–#22. CI passed, but the final manual diff/threat review remains an engineering task.

Do not declare production readiness while any actionable task or external blocker remains.
