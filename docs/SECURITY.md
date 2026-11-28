# Security and privacy

## Scope

AlibiCall is an offline local simulation, not a telephony application. It must not initiate real calls, spoof carrier caller ID, access call history, or impersonate emergency services, banks, government bodies, or real companies.

## Permission policy

The app declares `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`, `USE_FULL_SCREEN_INTENT` and `WAKE_LOCK`, in addition to AndroidX Core's signature-protected package permission described below. Notification permission is requested only in context. Exact scheduling is available only after `canScheduleExactAlarms()` reports user-granted Alarms & reminders access; approximate mode does not require it. `VIBRATE` enables the notification channel's pattern; ringtone and vibration are user-controlled in Android channel settings through the schedule screen. The app imports no audio files and runs no background audio of its own; keypad tones play only while the call screen is open. CI inspects both final merged manifests and uses a separate, exact allowlist for debug and release. A library-added permission is allowed only with a written rationale; requested permission names, defined permission names and each defined permission's protection level are checked exactly.

The current merged-manifest allowlist contains only AndroidX Core's package-specific signature permission:

- Debug: `com.siddumoni.fakecall.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- Release: `com.siddumoni.fakecall.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

AndroidX Core's AAR manifest declares `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` with `android:protectionLevel="signature"` and requests that permission. It is used by `ContextCompat.RECEIVER_NOT_EXPORTED` as a compatibility guard for runtime receivers on Android versions before API 33. AlibiCall has `minSdk 35`, so that legacy fallback path cannot execute; the dependency's manifest declarations nevertheless remain in the merged manifest. The custom permission does not grant telephony, contacts, call-log, SMS, microphone, camera, storage or network access. Signature protection limits it to applications signed with the same certificate. CI verifies the exact build-variant name and rejects a change from `signature` protection.

`RECEIVE_BOOT_COMPLETED` restores pending alarms after a restart (see Restart recovery). Future permission additions require a separate reviewed PR and exact allowlist update.

`USE_FULL_SCREEN_INTENT` is declared by owner decision (2026-11-28). A full-screen intent is attached only when the user has turned on **Full-screen incoming call** (off by default) and Android reports that it is allowed. Google Play limits this permission to calling and alarm apps, so Play distribution would need a separate policy review. `WAKE_LOCK` is used only for the proximity screen-off lock while an answered call screen is visible with the speaker off.

## Accessibility service

`VolumeTriggerService` is optional and off until the user enables it in Android Accessibility settings, after an in-app explanation. It requests key-event filtering only: no window content and no accessibility events. It inspects volume-down presses, keeps at most the last five timestamps in memory, never consumes the key and stores or sends nothing. It is not exported and is protected by `BIND_ACCESSIBILITY_SERVICE`. It is not declared as an accessibility tool; Google Play restricts non-accessibility use of this API, so Play distribution would need a separate review.

## Realistic call screens

By owner decision the call screens and call notification look like an ordinary incoming call and carry no simulation label, and the caller name is shown on the lock screen. The app still places no calls, creates no call-log entry and has no telephony access. Setup screens state that every call is simulated.

No INTERNET, CALL_PHONE, READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS, SEND_SMS, RECORD_AUDIO, CAMERA, location, or broad external storage permissions. Foreground-service declarations require a separate technical justification before implementation.

## Restart recovery

`RECEIVE_BOOT_COMPLETED` is declared only to restore pending alarms. `ScheduleRestoreReceiver` is not exported, handles only boot-completed, package-replaced, exact-alarm permission change, clock change and timezone change actions, and logs no record details.

## Component exposure

Every component is non-exported except the launcher Activity, which ignores intent data. The call screen Activity, the accessibility service and the scheduled-alarm, timeout and notification-dismiss receivers are explicitly non-exported. Alarm and notification action PendingIntents are explicit and immutable, and use unique data URIs per schedule. Notification content opens the non-exported call screen with the app-specific `alibicall://scheduled-call/<UUID>` URI; it accepts only the exact scheme/authority and a validated UUID path segment, optionally followed by exactly `answer` (or a `preview` URI with a caller ID), then reloads schedule state and caller data from private storage. Over the lock screen it offers call controls only, with no way into the rest of the app.

## Data protection

Caller presets, schedules and the onboarding acknowledgement belong in app-private storage. The onboarding preference contains only a boolean and does not request or collect personal details. Android cloud backup and device transfer are disabled. Cleartext traffic is disabled. No network permission, analytics, ads, tracking, crash reporting or remote configuration is permitted.

## Input validation

Treat all Intent extras, notification action identifiers, timestamps and text fields as untrusted. Resolve caller records from local storage using validated opaque identifiers. Caller display names reject Unicode bidirectional formatting controls to reduce visual-order spoofing; ordinary right-to-left text remains allowed. The app does not import images, audio or other files; adding that requires bounded decoding, revoked-grant handling and a new review. Do not log caller details or phone-like strings.

## Build and release

Release builds must be non-debuggable, minified and resource-shrunk. Never commit signing keys, passwords, API tokens or keystores. GitHub Actions should use least-privilege permissions, full commit SHA pins for third-party actions, no pull_request_target execution of untrusted PR code, and no signing secrets in PR workflows.

## Vulnerability reporting

Report security issues privately to the repository owner through GitHub's private vulnerability reporting feature where available. Do not publish exploit details before a fix is available.

## Review status

Source review on 2026-11-28 at `ebd9df8`, before the accessibility service and full-screen call screen were added (manifest, receivers, PendingIntents, launch-link handling, storage, backup, logging and workflow permissions). This was not an independent audit or a physical-device test.

Confirmed:

- Only the launcher Activity is exported.
- All PendingIntents are explicit and immutable.
- Backup and device transfer are excluded; there is no network permission.
- The only app log message is a generic restore failure with no record data.
- Third-party actions are pinned to full commit SHAs; the release job is the only active job that can write repository contents.

Open items:

- Caller names are free text by owner decision (2026-11-28). Length, control and bidirectional characters are checked.
