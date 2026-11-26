# AlibiCall

AlibiCall is a private, offline Android app that lets its owner schedule a locally simulated incoming call.

## Project status

The repository has been created. The initial Android project and CI foundation are being established. See [docs/STATUS.md](docs/STATUS.md) for verified progress.

## Security model

- No internet, analytics, advertising, tracking, or remote configuration.
- No real telephony, contacts, call logs, SMS, microphone, camera, location, or broad storage access.
- Caller presets and schedules remain on-device.
- Simulated calls must never be confused with real telephony.

## Planned platform

- Kotlin, Jetpack Compose, Material 3
- Application ID: `com.siddumoni.fakecall`
- minSdk 34, compileSdk 37, targetSdk 37

## Development

See [docs/DESIGN.md](docs/DESIGN.md), [docs/SECURITY.md](docs/SECURITY.md), and [docs/DEVICE_TESTS.md](docs/DEVICE_TESTS.md).

The device test checklist must be completed on physical hardware; CI cannot validate ColorOS-specific behaviour.
