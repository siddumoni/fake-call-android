# Physical device validation checklist

Target: OnePlus Nord 6 (CPH2793), India, ColorOS 17, Android 17 / API 37.

The development environment cannot validate this physical device. Mark each item only after a real test and record app version, OS build, date and outcome. Do not infer an outcome from emulator tests.

## First-run onboarding

- [ ] Fresh install opens the privacy explanation before Home.
- [ ] Offline-only, fictional-profile and no-real-calls statements are readable with TalkBack.
- [ ] Continue advances only after the local acknowledgement is saved.
- [ ] A storage failure keeps onboarding visible and displays an error.
- [ ] System Back exits onboarding instead of opening Home.
- [ ] Force-stop and relaunch after acknowledgement opens Home without repeating onboarding.
- [ ] Onboarding does not request notification, alarm, contacts, telephony or other permissions.

## Before testing

- Install the debug APK from the CI artifact.
- Record Android build fingerprint and ColorOS build.
- Grant notification permission and exact-alarm access only when prompted by the app.
- Confirm the app does not request USE_FULL_SCREEN_INTENT and uses heads-up / notification fallback instead.
- Confirm the test profile is fictional and does not resemble an emergency service, bank, authority or real business.

## Call delivery

- [ ] Device locked: schedule a call; verify notification/call presentation and actions.
- [ ] Device unlocked with another app in foreground: verify heads-up fallback; ensure no forced takeover.
- [ ] Screen off: verify schedule delivery.
- [ ] App open: verify delivery.
- [ ] App swiped away: verify delivery.
- [ ] App process killed: verify delivery and recovery.
- [ ] Reboot with a pending schedule: verify only valid future calls are restored.
- [ ] Cancel before trigger: verify no call notification appears.
- [ ] Repeated Answer/Decline/Dismiss: verify idempotent state transitions.
- [ ] Ring timeout: verify ringing stops and state becomes TimedOut.

## Audio and interruption modes

- [ ] Normal ringtone profile.
- [ ] Silent mode.
- [ ] Vibrate mode.
- [ ] Do Not Disturb with relevant system policy.
- [ ] Battery saver.
- [ ] Bluetooth headset connected.
- [ ] Volume changed during notification.
- [ ] Notification channel sound disabled by user.
- [ ] Custom SAF audio selection, if supported by implementation.
- [ ] Confirm no duplicate ringing.
- [ ] Run strict audio test: `adb shell cmd audio set-enable-hardening throw`; capture logs and verify expected failures are handled. This mode is stricter than default and removes the exact-alarm audio exemption.
- [ ] Restore default audio hardening after the test.

## Permission failure and revocation

- [ ] Notifications denied when creating a schedule; verify schedule is not silently created without delivery.
- [ ] Notifications revoked from system settings after a schedule exists; verify no Activity is launched from background and state is recorded safely.
- [ ] Exact alarm access denied; exact schedule is blocked with clear explanation.
- [ ] Exact alarm access revoked while a schedule exists; verify exact mode is not downgraded silently.
- [ ] Approximate schedule remains available without exact-alarm special access.
- [ ] Verify heads-up / lock-screen notification fallback; app must not request full-screen intent access.
- [ ] Return from settings and confirm permission-health state refreshes.

## Themes and accessibility

For Follow system, Light, Dark, and each supported combination of Pure black and Dynamic colour:
- [ ] Cold start and splash screen.
- [ ] Warm start.
- [ ] Main screen, settings, dialogs, menus, bottom sheets and date/time picker.
- [ ] Incoming-call screen and in-call screen.
- [ ] Status bar and navigation bar.
- [ ] Notification and quick settings tile (when implemented).
- [ ] Change system theme while app remains open.
- [ ] Change app theme while navigating; verify screen state is retained.
- [ ] Font scaling at 200%.
- [ ] TalkBack labels and traversal.
- [ ] Touch targets are at least 48 dp.
- [ ] Reduced motion preference.
- [ ] Predictive back and edge-to-edge.

## OEM reliability settings

The names and exact paths of ColorOS 17 battery optimisation, auto-launch, lock-screen notification and pop-up permissions are not independently verified yet. Capture the displayed labels on the target handset before adding exact instructions here.

- [ ] Battery optimisation state recorded.
- [ ] Auto-launch/background launch state recorded, if present.
- [ ] Lock-screen notifications state recorded.
- [ ] Pop-up/full-screen display setting state recorded, if present.
