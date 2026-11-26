# Project status

Last updated: 2026-11-26

## Verified

- Public repository created: https://github.com/siddumoni/fake-call-android
- Default branch: main.
- Initial commit created on main: repository README only.
- Working branch created: initial-setup.
- Android 17 official docs reviewed for target-SDK behaviour changes, background audio hardening, exact alarms, and full-screen-intent fallback.
- Compose BOM documentation currently specifies 2026.11.01.
- Candidate toolchain pinned in version catalog: AGP 9.2.0, Gradle 9.4.1, Kotlin 2.4.21, JDK 17. AGP 9.2.0 release notes identify API 37 support and Gradle 9.4.1 minimum. Full Gradle/Kotlin/AGP build compatibility still needs a CI run.

## Current implementation scope

PR 1 is the foundation only: one application module, pinned version catalog, no-permission starting manifest, offline security defaults, central starter theme, source-manifest policy test, and project/security/device documentation. No incoming-call features are claimed implemented.

## Not yet verified

- No Gradle build has run in this session.
- No unit or instrumented tests have run.
- No emulator API 37 test has run.
- No screenshot tests have run.
- No lint, ktlint, CodeQL, dependency review, secret scan, or merged-manifest allowlist CI has completed.
- No physical OnePlus Nord 6 / ColorOS 17 validation has been performed.
- CI action SHA pins and dependency verification metadata still need to be added and checked.
- No signed APK or release exists.

## Next steps

1. Complete PR 1 CI/security workflows, including full-SHA action pins, Gradle wrapper validation, dependency verification, merged-manifest permission allowlist, lint and formatting.
2. Add required unit and emulator/instrumented test jobs using the highest available API image and explicitly report its API.
3. Open PR 1 and wait for every required check to finish successfully on its latest commit.
4. Perform hostile self-review; fix findings without weakening checks.
5. Merge only after all required checks are complete and passing.
6. Configure repository ruleset and GitHub security settings manually because the current GitHub connector does not expose repository-settings mutation.
7. Proceed to PR 2: theme system and dark mode with screenshot tests.

## Blockers and limitations

- The GitHub connector cannot create releases or modify repository secrets/settings. Release publication and protection settings may require owner UI steps.
- Gradle wrapper scripts/JAR and dependency-verification metadata must be generated from a real Gradle distribution and validated before claiming wrapper integrity.
- Android SDK/API 37 emulator availability depends on CI image availability; report the selected API level and any limitation.
- ColorOS settings labels require the physical phone.
