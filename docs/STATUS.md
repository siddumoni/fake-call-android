# Project status

Last updated: 2026-11-26

## Verified in GitHub

- Public repository: https://github.com/siddumoni/fake-call-android
- Default branch: `main`
- Initial commit on `main`: README only.
- Working branch: `initial-setup`; all subsequent changes remain off main.
- The original scaffold was incomplete: no app sources, no version catalog, no root build script and no Gradle wrapper; its module build referenced missing aliases and a custom plugin. PR 1 replaces that foundation.

## Toolchain

- AGP 9.3.1, Gradle 9.7.0 and Kotlin 2.4.21 stay within Kotlin's documented fully supported Gradle / AGP compatibility range.
- Compose BOM 2026.09.00, selecting the latest stable Android Developers-documented BOM compatible with compileSdk 37. Compose 1.13.0 requires 37.1 and is not used.; Core 1.19.1; Activity Compose 1.13.0; Lifecycle 2.11.1; Preferences DataStore 1.3.0.
- compileSdk / targetSdk 37, minSdk 34, JDK 17.

Newer standalone AGP and Gradle versions exist, but they are outside the documented fully supported KGP 2.4.21 range. Compatibility was preferred over choosing newer version numbers that the Kotlin team does not list as fully supported.

## PR 1 scope

Project foundation, version catalog, no-permission manifest, Compose startup shell, unit and screenshot smoke tests, Gradle wrapper bootstrap / validation, dependency verification, Android lint, ktlint, CodeQL, dependency review, secret scanning, merged-manifest allowlist check, Dependabot and release workflow.

## Test evidence

No Android build or test result is claimed yet. A trusted push workflow generates the official Gradle wrapper and dependency verification metadata, then runs build, tests and lint before committing the generated integrity files. PR 1 must subsequently pass every required check on its latest head SHA.

## Not yet verified

- No successful Android build/test/CI result yet.
- No physical OnePlus Nord 6 / ColorOS 17 validation.
- No release signing secrets or signed release artifact.
- Repository rulesets, secret scanning and push protection need owner-side settings because current connector actions do not expose settings writes.

## Next steps

1. Let the wrapper-bootstrap workflow generate and verify the official wrapper and checksums.
2. Fix bootstrap build, unit-test, lint or formatting failures without weakening checks.
3. Open PR 1; inspect every required check on the latest head SHA and perform a hostile diff review.
4. Merge only after all required checks have completed and passed.
5. Apply repository ruleset and security settings.
6. Continue with PR 2 (persistent theme and dark-mode system), then the remaining feature PRs.

## Physical-device limitation

An Android emulator cannot validate ColorOS-specific alarm delivery, lock-screen presentation, OEM battery controls or background audio. Those remain physical-handset checks.
