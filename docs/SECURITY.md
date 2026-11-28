# Security and privacy

## Scope

AlibiCall is an offline local simulation, not a telephony application. It must not initiate real calls, spoof carrier caller ID, access call history, or impersonate emergency services, banks, government bodies, or real companies.

## Permission policy

The app declares `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` and `VIBRATE` for the scheduled-simulation feature, in addition to AndroidX Core's signature-protected package permission described below. Notification permission is requested only in context. Exact scheduling is available only after `canScheduleExactAlarms()` reports user-granted Alarms & reminders access; approximate mode does not require it. `VIBRATE` enables the notification channel's pattern; ringtone and vibration are user-controlled in Android channel settings through the schedule screen. The app imports no audio files and runs no background audio of its own. CI inspects both final merged manifests and uses a separate, exact allowlist for debug and release. A library-added permission is allowed only with a written rationale; requested permission names, defined permission names and each defined permission's protection level are checked exactly.

The current merged-manifest allowlist contains only AndroidX Core's package-specific signature permission:

- Debug: `com.siddumoni.fakecall.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- Release: `com.siddumoni.fakecall.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

AndroidX Core's AAR manifest declares `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` with `android:protectionLevel="signature"` and requests that permission. It is used by `ContextCompat.RECEIVER_NOT_EXPORTED` as a compatibility guard for runtime receivers on Android versions before API 33. AlibiCall has `minSdk 35`, so that legacy fallback path cannot execute; the dependency's manifest declarations nevertheless remain in the merged manifest. The custom permission does not grant telephony, contacts, call-log, SMS, microphone, camera, storage or network access. Signature protection limits it to applications signed with the same certificate. CI verifies the exact build-variant name and rejects a change from `signature` protection.

Future permission additions require a separate reviewed PR and exact allowlist update:

| Permission | Purpose | Gate |
|---|---|---|
| RECEIVE_BOOT_COMPLETED | Restore valid pending alarms after reboot | Declared; non-exported receiver |

`USE_FULL_SCREEN_INTENT` is deliberately not declared: AlibiCall is a fictional local simulation, not a phone/video calling app or a conventional alarm-clock app. It uses a high-importance notification with the system heads-up/lock-screen presentation instead of requesting restricted full-screen takeover. VIBRATE is limited to notification-channel vibration; no custom background audio capability is requested.

No INTERNET, CALL_PHONE, READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS, SEND_SMS, RECORD_AUDIO, CAMERA, location, or broad external storage permissions. Foreground-service declarations require a separate technical justification before implementation.

## Restart recovery

`RECEIVE_BOOT_COMPLETED` is declared only to restore pending alarms. `ScheduleRestoreReceiver` is not exported, handles only boot-completed, package-replaced and exact-alarm-grant actions, and logs no record details.

## Component exposure

Every component is non-exported by default except the launcher Activity. The scheduled-alarm, timeout and notification-dismiss receivers are explicitly non-exported. Alarm and notification action PendingIntents are explicit and immutable, and use unique data URIs per schedule. Notification content opens the launcher Activity with the app-specific `alibicall://scheduled-call/<UUID>` URI; the Activity accepts only ACTION_VIEW with the exact scheme/authority and a validated UUID path segment, optionally followed by exactly `answer`, then reloads schedule state and caller data from private storage. It does not trust intent extras.

## Data protection

Caller presets, schedules and the onboarding acknowledgement belong in app-private storage. The onboarding preference contains only a boolean and does not request or collect personal details. Android cloud backup and device transfer are disabled. Cleartext traffic is disabled. No network permission, analytics, ads, tracking, crash reporting or remote configuration is permitted.

## Input validation

Treat all Intent extras, notification action identifiers, timestamps, text fields and content URIs as untrusted. Resolve caller records from local storage using validated opaque identifiers. Caller display names reject Unicode bidirectional formatting controls to reduce visual-order spoofing; ordinary right-to-left text remains allowed. Bound image dimensions, file size and audio duration. Handle revoked SAF grants and decode errors safely. Do not log caller details, phone-like strings, user-selected paths or content.

## Build and release

Release builds must be non-debuggable, minified and resource-shrunk. Never commit signing keys, passwords, API tokens or keystores. GitHub Actions should use least-privilege permissions, full commit SHA pins for third-party actions, no pull_request_target execution of untrusted PR code, and no signing secrets in PR workflows.

## Vulnerability reporting

Report security issues privately to the repository owner through GitHub's private vulnerability reporting feature where available. Do not publish exploit details before a fix is available.

## Review status

The schedule lifecycle, alarm/notification delivery, reboot recovery and clock/timezone reconciliation are implemented in main. This document describes the design policy, not a claim that the finished app has passed an independent security audit or physical-device reliability testing. The final manual threat review and OEM validation are outstanding. Record findings and evidence in docs/STATUS.md.
