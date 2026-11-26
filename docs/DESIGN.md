# AlibiCall design

## Purpose and non-goals

AlibiCall schedules a clearly local, simulated incoming call on the device owner's phone. It does not place calls, spoof cellular caller ID, create call-log entries, or interact with telephony. It is intended as a personal convenience and must not impersonate emergency services, public authorities, banks, government agencies, or real businesses.

## Architecture

Single Android application module. The UI uses Jetpack Compose Material 3 and follows MVVM with unidirectional data flow. UI state is exposed as immutable StateFlow values; actions are sent to ViewModels. Preferences and small records are stored in app-private DataStore. User-selected images/audio are accessed through Storage Access Framework or Android Photo Picker content URIs. Persistable URI grants must be validated and revoked when no longer needed.

The app is offline-only. There is no networking, analytics, crash reporting, advertising, remote configuration, or server component.

## Call lifecycle

A scheduled call is a local state machine: Draft -> Scheduled -> Ringing -> InCall -> Ended, with Cancelled and TimedOut terminal alternatives. State transitions must be idempotent. An incoming-call Activity is isolated from normal navigation and receives only a validated opaque schedule identifier; it reloads caller data from app-private storage. Never trust caller text, number, media URI, or timing fields delivered as arbitrary intent extras.

Scheduling uses AlarmManager exact alarms only after checking canScheduleExactAlarms(). Permission denial or revocation is an explicit UI state; do not silently downgrade a user-requested exact schedule to an inaccurate one. Re-check permission after returning from system settings and restore only still-valid schedules after boot. Persist the schedule before setting the alarm and make receiver handling idempotent.

Incoming-call presentation uses Notification.CallStyle and a full-screen intent only after checking NotificationManager.canUseFullScreenIntent(). Without full-screen access, rely on the system's heads-up / lock-screen notification presentation. While the device is unlocked, expect the system to display a heads-up notification rather than forcibly replacing the foreground app. Answer, Decline and Dismiss actions are explicit immutable PendingIntents with distinct request codes and validated IDs.

## Audio decision

Prefer notification-channel-owned system ringtone playback to avoid starting an independent audio player from the background. Android notification channel sound is immutable after channel creation: ringtone choice must be designed around channel identity/versioning and user control in system settings. The platform may not be able to read an arbitrary SAF content URI as a notification-channel sound; validate this on the target API and OEM, and use only a documented, secure approach if custom ringtone support is proven. Never add broad storage permissions or export an unprotected receiver to solve URI access.

Do not run a second player while the notification channel is ringing. In-call voice clips, if implemented, must be explicitly started by a user action while a visible Activity is active, must stop on lifecycle exit, End, timeout, dismiss or process cleanup, and must not be used to simulate a real conversation deceptively. Avoid a foreground service unless the chosen feature has a demonstrable valid service type and Android 17 lifecycle exemption. Audio hardening strict mode removes the alarm-stream exemption and should be documented as a stricter test than default behaviour.

## Theme and UI

Theme preferences are Follow system, Light and Dark, plus independent Pure black (AMOLED) and Dynamic colour toggles. Persist preferences and apply app-level night mode early enough to avoid a cold-start flash. UI colours must be obtained from the central theme. Respect font scaling, TalkBack, 48 dp touch targets, reduced motion, predictive back, and edge-to-edge in every screen.

## Permissions

Request only a permission required by a shipped user-facing feature. Candidate permissions for later feature PRs: POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT, SCHEDULE_EXACT_ALARM, VIBRATE and RECEIVE_BOOT_COMPLETED. Foreground-service permissions are not approved by default; require a documented implementation need and API-level analysis first. CI must inspect the merged manifest, not only the source manifest, and compare all permissions with a reviewed allowlist.

## Threat model

Assets: caller preset data, schedule timing, local selected image/audio content, and control over the simulated call display.

Trust boundaries: external intents, notifications, OS settings, reboot broadcasts, SAF/Photo Picker URIs, and downloaded dependencies/build actions.

Threats and mitigations:
- Forged or stale intents: explicit intents, immutable PendingIntents, opaque IDs, private state lookup, strict state and time validation.
- Component abuse: non-exported components by default; exported launcher only; no exported receivers/services unless a platform entry point requires it.
- Data leakage: app-private storage, backup/device transfer disabled, no network stack permission, no sensitive logs.
- Malformed media: enforce size/type limits, handle revoked URI grants and decode failures, never load unbounded bitmaps/audio into memory.
- Dependency compromise: dependency locking/verification, Dependabot, CodeQL, secret scanning and pinned CI action SHAs.
- Confusing simulation with reality: visible simulated-call wording in app documentation/settings; no telephony integration or emergency/company impersonation.

## Known platform risks

Android 17 background audio hardening may silently reject audio operations outside a valid lifecycle. Exact alarm exemption applies only under documented conditions and does not survive strict hardening test mode. Full-screen intent availability depends on system approval. OEM battery and auto-launch policies may delay or suppress alarms/notifications. Notification channel sound is user-controlled and immutable after channel creation. These behaviours require physical-device validation.

ColorOS-specific setting names and paths have not yet been confirmed on the target device. Verify before documenting a precise menu path; otherwise mark it unverified in DEVICE_TESTS.md.
