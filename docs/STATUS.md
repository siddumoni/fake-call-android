# Project status

Last updated: 2026-11-28

## Repository state

- Repository: https://github.com/siddumoni/fake-call-android
- Latest merge covered here: PR #30 at `ebd9df8`.
- Minimum OS: Android 15 (API 35); target SDK 37; compile platform Android 17 API 37.1.
- Pull requests and `main` run build/unit/lint/manifest policy, CodeQL, secret scan, OSV and instrumented tests on seven configurations across API 35/36/37 (compact, standard, expanded, landscape and 200% compact font scale).
- Production signing secrets are not configured and no release has been published.

## Toolchain

- Android Gradle Plugin 9.3.1; Gradle 9.7.0; Kotlin / Compose compiler plugin 2.4.21; JDK 17.
- Compose BOM 2026.11.01; compile API 37.1; target SDK 37; minimum SDK 35.
- The emulator matrix targets Android 15 (API 35), 16 (API 36) and 17 (API 37), including compact landscape, expanded and compact 200% font scale.

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

## Verified CI

| PR | Merge SHA | Result |
|---|---|---|
| #19 Schedule UI, alarms and notifications | `28d2ded` | PR checks passed |
| #20 Restart/package-update recovery | `4dd642a` | PR checks passed |
| #21 Android 15 minimum/layout fixes | `1b53359` | PR checks passed |
| #22 Clock/timezone reconciliation | `b054770` | PR checks passed |
| #24 Elapsed-realtime ring timeout | `8015b9e` | PR checks passed |
| #25 Notification sound/vibration settings | `5e0efd3` | PR checks passed |
| #23 Seven-configuration emulator matrix | `6b919b6` | PR and `main` checks passed |
| #26 Notification Answer/Decline, responsive call screen | `81bdaca` | PR and `main` checks passed |
| #27 Looping system ringtone | `2724603` | PR and `main` checks passed |
| #28 Release safety, dependency scan on `main` | `87d9319` | PR and `main` checks passed |
| #29 Large-font layouts and headings | `a8a5ff7` | PR and `main` checks passed |
| #30 Alarm delivery tests | `ebd9df8` | PR checks passed (one job rerun after a plugin download error) |

## Remaining actionable work

1. Alarm delivery, duplicate delivery, cancellation, deleted caller and revoked notifications are covered on emulators. Exact-alarm revocation, real process death and storage-failure tests are still outstanding.
2. Accessibility: titles are marked as headings and screens scroll at large fonts (emulator-tested). Manual TalkBack traversal, touch-target, contrast, predictive Back and reduced-motion review is still outstanding.
3. Source security review recorded in `docs/SECURITY.md` with its open items. Verify the signed release artifact once signing secrets exist.
4. Configure production signing using owner-provided secrets. Automation must not create, invent or commit a keystore/password.
5. Run physical-device tests across multiple OEMs (OnePlus/ColorOS, Samsung, Pixel/AOSP), including ringtone routing, silent/Do Not Disturb and lock-screen behaviour.
6. Owner decision: delete merged branches, and retire or retarget `gradle-integrity.yml`, which only runs for the old `initial-setup` branch.

## Dependency maintenance

Dependabot PRs #3–#7 are merged. PR #8 (Gradle 9.9.2) and #9 (AGP 9.5.3) were closed after compatibility review: Kotlin Gradle Plugin 2.4.21's published fully supported maxima are Gradle 9.7.0 and AGP 9.3.1 respectively. Both updates also failed strict dependency verification on new artifacts. Revisit only as a coordinated toolchain upgrade with reviewed SHA-256 metadata and full green CI.

## External blockers

- No physical Android device is connected. Alarm delivery under OEM battery restrictions, lock-screen behavior, reboot, timezone/clock changes, audio routing, font scaling and OEM settings remain unverified. Emulator results are not physical-device evidence.
- Production signing requires owner-provided keystore and GitHub Actions secrets.
- Automated Codex reviews for PRs #19–#24 reported account usage limits.

Do not declare production readiness while actionable tasks or external blockers remain.
