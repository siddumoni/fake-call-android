# Project status

Last updated: 2026-11-27

## Repository state

- Repository: https://github.com/siddumoni/fake-call-android
- Latest verified main merge: PR #24 at `8015b9e84878e9e757e0cd76911a92e2f734c4d7`.
- Minimum OS: Android 15 (API 35); target SDK 37; compile platform Android 17 API 37.1.
- PRs #19–#22 and #24 are merged. Their PR-triggered Android CI runs passed all required jobs.
- No open issues were returned by the repository audit. PR #23 (compatibility matrix) and PR #25 (notification controls) are open.
- Main CI currently compiles at API 37.1 and runs instrumented tests on Android 17 emulator only. PR #23 adds a six-entry API 35/36/37, display and font-scale matrix; its earlier runs failed and the latest code fixes have not yet completed CI.
- Production signing secrets are not configured.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 35.
- PR #23 matrix: Android 15 (API 35), 16 (API 36), 17 (API 37); compact (360 dp), standard (~411 dp), expanded (600 dp); compact at 200% font scaling.

## Merged implementation

### Foundation, theme and privacy

- Debug/release variants, wrapper validation, strict dependency verification, build/unit/lint/manifest policy, CodeQL, Gitleaks and OSV.
- Preferences DataStore-backed Follow system / Light / Dark, Pure Black (AMOLED) and Dynamic colour.
- Material 3 palettes, contrast fallback, edge-to-edge and startup theme application.
- Caller-profile CRUD and local simulated-call preview with fictional names and optional number labels.
- Privacy-first onboarding, local DataStore storage, no real telephony/call logs/network communication.
- Caller editor scrolls and retains input across rotation/theme changes.

### Schedule lifecycle and delivery

- Schedule model, UUID/time-horizon validation, DST resolution, versioned codec and app-private DataStore persistence.
- Create/edit/cancel/delete UI; exact and approximate delivery modes.
- Explicit exact-alarm permission flow with no silent downgrade.
- AlarmManager trigger/timeout, unique immutable PendingIntents, non-exported receivers.
- Private high-importance heads-up notification with generic public version, Dismiss action and strict app URI navigation. No full-screen-intent permission.
- Save rollback, cancellation fencing, duplicate-save guard and reconciliation.
- PR #19 merged at `28d2ded7745d3f80c3b0cee25d4c0ed4c5d4c834`.

### Recovery and time changes

- Non-exported receiver restores valid pending schedules after reboot, package replacement and exact-alarm grant; closes expired/interrupted records.
- Clock/timezone changes reconcile absolute trigger instants; overdue pending records inside grace are re-armed, expired pending records are closed.
- PR #20 merged at `4dd642a92a2f9606e4281c3fd165bb89b0558043`.
- Android 15 minimum and layout fixes in PR #21 merged at `1b5335902fede57c1a2f8ead3ea282ca04fce4c3`.
- Clock/timezone reconciliation in PR #22 merged at `b054770e90b515eae65c638e57a9633cbf9fbb2d`.
- Ring timeout uses `ELAPSED_REALTIME_WAKEUP`, converted from a wall-clock deadline at registration. Reconciliation no longer expires active RINGING records against their original schedule time. PR #24 merged at `8015b9e84878e9e757e0cd76911a92e2f734c4d7`.

## Verified PR CI

| PR | Merge SHA | CI run | Result |
|---|---|---|---|
| #19 Schedule UI, alarms and notifications | `28d2ded` | 45987430593 | All jobs green |
| #20 Restart/package-update recovery | `4dd642a` | 45988349158 | All jobs green |
| #21 Android 15 minimum/layout fixes | `1b533590` | 45989170051 | All jobs green |
| #22 Clock/timezone reconciliation | `b054770` | 45990412713 | All jobs green |
| #24 Elapsed ring timeout | `8015b9e` | 45994053187 | All jobs green |

Build/unit/lint/manifest, emulator instrumentation/screenshots, CodeQL, secret scan and OSV review succeeded in these runs. OSV's optional code-scanning export steps are skipped by configuration; the scanner job itself passed.

The connector cannot independently retrieve push-triggered post-merge runs on main; its workflow-run lookup is PR-only and combined-status results are empty.

## Remaining actionable work

1. PR #23: complete and merge the Android 15/16/17 emulator matrix. Initial compact/200% font tests exposed inaccessible offscreen test actions and a schedule-screen layout overflow; tests now scroll and the schedule screen was changed to one scrollable LazyColumn. The emulator action also needed the Android 17 preview platform minor version. The corrected run must finish fully green.
2. PR #25: configure notification channel default sound/vibration and add the system settings shortcut. CI is pending; custom ringtone files are not supported.
3. Expand lifecycle tests for notification denial/revocation, exact-alarm revocation, notification action paths, cancellation races, storage errors and restore edge cases.
4. Accessibility review across all screens: TalkBack traversal, semantics, 48 dp targets, 200% fonts, contrast and reduced motion.
5. Complete manual security/threat review; verify release minification, artifact, signature and checksum.
6. Configure production signing using owner-provided secrets. Automation must not create, invent or commit a keystore/password.
7. Run physical-device tests across multiple OEMs (OnePlus/ColorOS, Samsung, Pixel/AOSP) when hardware is available.
8. Compare stale branches with main and delete only those proven obsolete. Branch deletion is not exposed by the current connector.

## Dependency maintenance

Dependabot PRs #3–#7 are merged. PRs #8 (Gradle 9.9.2) and #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## External blockers

- No physical Android device is connected. Alarm delivery under OEM battery restrictions, lock-screen presentation, reboot, timezone/clock changes, audio routing, font scaling and manufacturer-specific settings remain unverified. Emulator tests are not physical-device evidence.
- Production signing requires owner-provided keystore and GitHub Actions secrets.
- Automated Codex reviews for PRs #19–#24 reported account usage limits. CI passed, but a final manual diff/threat review remains.

Do not declare production readiness while actionable tasks or external blockers remain.
