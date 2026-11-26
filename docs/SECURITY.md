# Security and privacy

## Scope

AlibiCall is an offline local simulation, not a telephony application. It must not initiate real calls, spoof carrier caller ID, access call history, or impersonate emergency services, banks, government bodies, or real companies.

## Permission policy

The initial foundation declares no Android permissions. Later features may add only individually reviewed permissions from this candidate set:

| Permission | Purpose | Gate |
|---|---|---|
| POST_NOTIFICATIONS | Incoming-call / schedule notifications | Request at runtime on applicable versions after explanation |
| USE_FULL_SCREEN_INTENT | Full-screen simulated-call UI | Check canUseFullScreenIntent and provide heads-up fallback |
| SCHEDULE_EXACT_ALARM | User-specified precise call schedule | Check canScheduleExactAlarms and handle revocation |
| VIBRATE | Optional vibration pattern | Confirm platform permission requirement at implementation time |
| RECEIVE_BOOT_COMPLETED | Restore valid pending alarms after reboot | Add only with tested boot receiver |

No INTERNET, CALL_PHONE, READ_PHONE_STATE, READ_CALL_LOG, READ_CONTACTS, SEND_SMS, RECORD_AUDIO, CAMERA, location, or broad external storage permissions. Foreground-service declarations require a separate technical justification before implementation.

## Component exposure

Every component is non-exported by default. The launcher Activity is the only currently exported component. Future notification actions, alarm receivers and reboot entry points must use explicit immutable PendingIntents and the narrowest possible exported state. The lock-screen Activity must not expose navigation to the main app or reveal anything other than the currently scheduled simulated caller.

## Data protection

Caller presets and schedules belong in app-private storage. Android cloud backup and device transfer are disabled. Cleartext traffic is disabled. No network permission, analytics, ads, tracking, crash reporting or remote configuration is permitted.

## Input validation

Treat all Intent extras, notification action identifiers, timestamps, text fields and content URIs as untrusted. Resolve caller records from local storage using validated opaque identifiers. Bound image dimensions, file size and audio duration. Handle revoked SAF grants and decode errors safely. Do not log caller details, phone-like strings, user-selected paths or content.

## Build and release

Release builds must be non-debuggable, minified and resource-shrunk. Never commit signing keys, passwords, API tokens or keystores. GitHub Actions should use least-privilege permissions, full commit SHA pins for third-party actions, no pull_request_target execution of untrusted PR code, and no signing secrets in PR workflows.

## Vulnerability reporting

Report security issues privately to the repository owner through GitHub's private vulnerability reporting feature where available. Do not publish exploit details before a fix is available.

## Review status

This document describes the design policy, not a claim that the finished app has passed a security audit. The initial project is not yet feature-complete or independently penetration-tested. All remaining findings and test evidence must be recorded in docs/STATUS.md.
