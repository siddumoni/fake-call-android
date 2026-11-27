# AlibiCall design

## Purpose and non-goals

AlibiCall schedules a clearly local, simulated incoming call on the device owner's phone. It does not place calls, spoof cellular caller ID, create call-log entries, or interact with telephony. It is intended as a personal convenience and must not impersonate emergency services, public authorities, banks, government agencies, or real businesses.

## Architecture

Single Android application module. The UI uses Jetpack Compose Material 3 and follows MVVM with unidirectional data flow. UI state is exposed as immutable StateFlow values; actions are sent to ViewModels. Preferences and small records are stored in app-private DataStore. User-selected images/audio are accessed through Storage Access Framework or Android Photo Picker content URIs. Persistable URI grants must be validated and revoked when no longer needed.

The app is offline-only. There is no networking, analytics, crash reporting, advertising, remote configuration, or server component.

## First-run onboarding

On a fresh install, the launcher presents a privacy-first onboarding screen before Home. It explains that the app is offline-only, caller profiles are fictional display details, and the app does not make real calls, spoof caller ID, access contacts or call logs, or impersonate people or organisations. The screen does not request permissions. A single acknowledgement boolean is stored in app-private Preferences DataStore. Home is shown only after the write succeeds; a storage failure is displayed and leaves onboarding visible. The system Back action exits rather than silently bypassing the acknowledgement. The content is regular Compose text and a labelled button so TalkBack can read it.

## Call lifecycle

A scheduled simulation is a local state machine. The persisted model supports SCHEDULED → RINGING → IN_CALL → ENDED, with CANCELLED and TIMED_OUT terminal alternatives. State transitions are action-based and atomically applied by the DataStore repository, so duplicate alarm delivery and illegal transitions are rejected. Notification content uses an explicit immutable PendingIntent to the exported launcher Activity with a strict app URI containing only a validated opaque schedule ID. The Activity reloads schedule state and caller data from app-private storage; it rejects arbitrary schemes, authorities, paths and extras. No external caller text, number, media URI, or timing data is trusted.

Scheduling uses AlarmManager exact alarms only after checking canScheduleExactAlarms(). Permission denial or revocation is an explicit UI state; do not silently downgrade a user-requested exact schedule to an inaccurate one. Approximate schedules use setAndAllowWhileIdle and can be delayed by power management. Re-check permission after returning from system settings and restore only still-valid schedules after boot. Persist the schedule before setting the alarm and make receiver handling idempotent.

Incoming-call presentation uses a high-importance notification with a clear SIMULATED CALL / NO REAL CALL label, private lock-screen visibility and a generic public version. A per-schedule immutable notification action dismisses the simulated call. This app intentionally does not request USE_FULL_SCREEN_INTENT: its content is a fictional call simulation, not an actual phone/video call or a conventional alarm clock. While unlocked, use system heads-up presentation; never forcibly replace the user's foreground app. Notification denial or revocation is handled as an explicit unavailable state, not by launching an Activity from the background.

## Audio decision

Prefer notification-channel-owned system ringtone playback to avoid starting an independent audio player from the background. Android notification channel sound is immutable after channel creation: ringtone choice must be designed around channel identity/versioning and user control in system settings. The platform may not be able to read an arbitrary SAF content URI as a notification-channel sound; validate this on the target API and OEM, and use only a documented, secure approach if custom ringtone support is proven. Never add broad storage permissions or export an unprotected receiver to solve URI access.

Do not run a second player while the notification channel is ringing. In-call voice clips, if implemented, must be explicitly started by a user action while a visible Activity is active, must stop on lifecycle exit, End, timeout, dismiss or process cleanup, and must not be used to simulate a real conversation deceptively. Avoid a foreground service unless the chosen feature has a demonstrable valid service type and Android 17 lifecycle exemption. Audio hardening strict mode removes the alarm-stream exemption and should be documented as a stricter test than default behaviour.

## Caller profiles and local preview (PR 3)

Caller profiles are stored as a string set in app-private Preferences DataStore. Each record has a UUID, a display name and an optional number label. Names are trimmed and limited to 40 characters; number labels are limited to 24 characters and accept only simple phone-display characters. The number label is never used for dialing. A URL-safe Base64 codec protects delimiters in stored text, and malformed entries are ignored when reading. Editing replaces the record with the same UUID; deleting removes only that profile.

The Caller Profiles screen supports add, edit and delete. Preview opens a Compose-only incoming-call mock with a prominent SIMULATED CALL banner. Answer transitions to a local answered mock; Decline or End returns to Home. No phone calls, network calls, call-log entries, notifications, scheduling, or ringtone playback are implemented in this slice.

## Theme and UI

Theme preferences are Follow system, Light and Dark, plus independent Pure black (AMOLED) and Dynamic colour toggles. Persist preferences and apply app-level night mode early enough to avoid a cold-start flash. UI colours must be obtained from the central theme. Respect font scaling, TalkBack, 48 dp touch targets, reduced motion, predictive back, and edge-to-edge in every screen.

## Permissions

The scheduled delivery implementation declares POST_NOTIFICATIONS and SCHEDULE_EXACT_ALARM. Notification permission is requested in context before a schedule is saved; denial blocks a schedule requiring a visible notification. Exact alarm access is user-managed via Alarms & reminders and is requested only when the user selects Exact; switching to Approximate is an explicit user choice. USE_FULL_SCREEN_INTENT is intentionally omitted because this app simulates calls and does not implement actual phone/video calling or a conventional alarm-clock use case. RECEIVE_BOOT_COMPLETED may be added only together with tested reboot recovery. Foreground-service permissions are not approved by default; require documented implementation need and API-level analysis first. CI must inspect the merged manifest, not only the source manifest, and compare all permissions with a reviewed allowlist.

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


## Theme system (PR 2)

Theme preferences are stored in Preferences DataStore as an appearance enum (`follow_system`, `light`, `dark`) plus independent `pure_black` and `dynamic_color` booleans. The default is Follow system, Pure black off, Dynamic colour on. Unknown persisted appearance values fall back to Follow system.

The Application reads the persisted theme before the first Activity starts and calls `UiModeManager.setApplicationNightMode` so Android can select the matching day/night resource variant before drawing the splash screen. Light and Dark map to the explicit package night modes; Follow system uses the package's undefined/automatic mode so the device configuration remains the source of truth. Compose independently observes `isSystemInDarkTheme()` while Follow system is selected, so later system changes are applied live. This behaviour must be verified on the API 37 emulator and target ColorOS device.

The Compose theme owns the static Light, Dark and Pure Black palettes. Android Dynamic colour is used only on API 31+ when enabled; Pure Black takes precedence over Dynamic colour when Dark appearance is active. Text roles are checked at runtime against a 4.5:1 contrast threshold and choose the higher-contrast black or white fallback when the preferred text colour does not meet it. Automated tests exercise the static palette roles. Dynamic colour relies on Android's system-generated accessible tonal palettes.

The launcher Activity uses edge-to-edge window handling. Day/night window resources set matching status-bar icon contrast and splash backgrounds. The light splash resource uses the light surface; the night splash resource uses black to ensure a dark or AMOLED preference never starts with a white frame. The Compose surfaces then take over using the selected palette.

Theme changes update Compose state immediately, update the application night mode, and persist the selection. DataStore read failures fall back to the documented defaults; write failures are displayed in the settings screen rather than silently claimed as saved. Theme state is passed down as immutable data and actions flow back to the ViewModel.

The screenshot smoke suite captures both the Home and Appearance screens in Light, Dark and Pure Black mode. The suite validates successful rendering and non-empty capture dimensions; it is not a pixel-perfect golden-image comparison. Physical OnePlus/ColorOS splash, system-bar, wallpaper colour and live system-theme behaviour remains a device test.
