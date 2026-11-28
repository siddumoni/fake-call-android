# Project status

Last updated: 2026-11-27

## Repository state

- Repository: https://github.com/siddumoni/fake-call-android
- Latest main merge: PR #24 at `8015b9e84878e9e757e0cd76911a92e2f734c4d7`.
- Minimum OS: Android 15 (API 35); target SDK 37; compile platform Android 17 API 37.1.
- PRs #19–#24 are merged; PR #24 fixes the 45-second ring timeout to use elapsed realtime. Their PR-triggered CI passed all required jobs.
- No open issues were returned by the latest audit.
- Main CI currently compiles against API 37.1 and runs instrumentation on Android 17. PR #23 adds seven configurations across Android 15/16/17, compact/standard/expanded/landscape and 200% compact font scale. Its latest run is pending.
- Production signing secrets are not configured.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 35.
- PR #23's emulator matrix targets Android 15 (API 35), 16 (API 36) and 17 (API 37), including compact landscape, expanded and compact 200% font scale.

## Merged implementation

### Foundation, theme and privacy

- Debug/release variants, wrapper validation, strict dependency verification, build/unit/lint/manifest policy, CodeQL, Gitleaks and OSV.
- Preferences DataStore-backed Follow system / Light / Dark, Pure Black (AMOLED) and Dynamic colour.
- Material 3 palettes, contrast fallback, edge-to-edge and startup theme application.
- Caller-profile CRUD and local simulated-call preview with fictional names and optional number labels.
- Privacy-first onboarding, local DataStore storage, no real telephony/call logs/network communication.
- Caller/schedule editor input state retained across rotation/theme changes; caller editor scrolls.

### Schedule lifecycle and delivery

- Schedule model, UUID/time-horizon validation, DST resolution, versioned codec and app-private DataStore persistence.
- Create/edit/cancel/delete UI; exact and approximate delivery modes.
- Explicit exact-alarm permission flow with no silent downgrade.
- AlarmManager trigger/timeout, unique immutable PendingIntents, non-exported receivers.
- Private high-importance heads-up notification with generic public version, Answer and Decline actions, swipe-to-decline and strict app URI navigation. No full-screen intent permission.
- Save rollback, cancellation fencing, duplicate-save guard and reconciliation.
- PR #19 merged at `28d2ded7745d3f80c3b0cee25d4c0ed4c5d4c834`.

### Recovery

- Non-exported receiver restores valid pending schedules after reboot, package replacement and exact-alarm grant; closes expired/interrupted records.
- Clock/timezone changes reconcile absolute trigger instants; overdue records inside grace are re-armed, expired pending records are closed.
- PR #20 merged at `4dd642a92a2f9606e4281c3fd165bb89b0558043`.
- Android 15 minimum and editor/layout fixes in PR #21 merged at `1b5335902fede57c1a2f8ead3ea282ca04fce4c3`.
- Clock/timezone reconciliation in PR #22 merged at `b054770e90b515eae65c638e57a9633cbf9fbb2d`.

## Verified PR CI

| PR | Merge SHA | CI run | Result |
|---|---|---|---|
| #19 Schedule UI, alarms and notifications | `28d2ded` | 45987430593 | All jobs green |
| #20 Restart/package-update recovery | `4dd642a` | 45988349158 | All jobs green |
| #21 Android 15 minimum/layout fixes | `1b533590` | 45989170051 | All jobs green |
| #22 Clock/timezone reconciliation | `b054770` | 45990412713 | All jobs green |
| #24 Elapsed-realtime ring timeout | `8015b9e` | 45994053187 | All jobs green |

Build/unit/lint/manifest, emulator instrumentation/screenshots, CodeQL, secret scan and OSV review succeeded in these runs. OSV's optional code-scanning export steps are skipped by configuration; the scanner job itself passed.

Post-merge main CI run 45995506622 succeeded: build/unit/lint/manifest, API 37 instrumentation/screenshots, CodeQL and secret scan. OSV jobs are skipped for push events by workflow configuration.

## Remaining actionable work

1. PR #23 (7-configuration API 35/36/37 emulator matrix) merged at `6b919b6`.
2. PR #25 (notification sound/vibration channel settings) merged at `5e0efd3`.
3. Expand lifecycle tests for notification denial/revocation, exact-alarm revocation, cancellation races, storage errors and restore edge cases.
4. Accessibility review across all screens: TalkBack traversal, semantics, 48 dp targets, 200% fonts, contrast and reduced motion.
5. The system channel loops the ringtone; there is no app-owned player or file import. Physical routing/interruption behavior remains to be tested.
6. Complete manual security/threat review; verify release minification, artifact, signature and checksum.
7. Configure production signing using owner-provided secrets. Automation must not create, invent or commit a keystore/password.
8. Run physical-device tests across multiple OEMs (OnePlus/ColorOS, Samsung, Pixel/AOSP) when hardware is available.
9. Branch housekeeping: 13 branches with zero unique commits were safely fast-forwarded to main. Diverged branches were left untouched; branch deletion is not exposed by the current connector.

## Dependency maintenance

Dependabot PRs #3–#7 are merged. PR #8 (Gradle 9.9.2) and #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1 respectively. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## External blockers

- No physical Android device is connected. Alarm delivery under OEM battery restrictions, lock-screen behavior, reboot, timezone/clock changes, audio routing, font scaling and OEM settings remain unverified. Emulator results are not physical-device evidence.
- Production signing requires owner-provided keystore and GitHub Actions secrets.
- Automated Codex reviews for PRs #19–#24 reported account usage limits. Required CI passed, but a final manual diff/threat review remains.

Do not declare production readiness while actionable tasks or external blockers remain.
