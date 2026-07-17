# Modernization plan

After each stage: verify the build compiles, tests pass, and all 92 reports generate.

## Stage 1 — Upgrade JDK to 25

Kotlin 2.2.0 can't parse the Salesforce JDK's 5-part version string (`25.0.2.0.101`). Need a standard JDK 25.

- Install standard JDK 25: `brew install --cask temurin@25`
- Bump `pom.xml` compiler source/target and `kotlin.compiler.jvmTarget` to `25`
- Remove `JAVA_HOME` override from any build/run commands

## Stage 2 — Convert Gradle → Maven

Replace `build.gradle.kts`, `gradle/`, `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `gradle.properties` with a `pom.xml`.

- Preserve all existing dependencies (OkHttp, JUnit Jupiter, Kotlin stdlib)
- Preserve application entrypoint (`MainKt`)
- Preserve test config (JUnit Platform, heap sizes)
- Keep spotless or replace with equivalent Maven formatter plugin

## Stage 3 — Run MCU to update all dependencies

Run the `/zos:mcu` skill against the new `pom.xml` to get all deps to latest stable versions.

## Stage 4 — Fix all compiler warnings and ktlint violations

Build with `-Werror` (or equivalent) and resolve every warning:
- Kotlin deprecations
- Unchecked casts
- Unused imports / variables
- Any Java interop warnings

Upgrade ktlint to latest stable and fix all violations:
- Update ktlint version in `pom.xml` (via spotless or ktlint Maven plugin)
- Run ktlint check; fix all reported style violations
- Enforce ktlint in the build so violations fail the build going forward

Add a `make checks` target that runs:
- `spotless:check` — formatting violations
- CPD (Copy-Paste Detector via `maven-pmd-plugin`) — duplicate code detection
- Fix any violations found before proceeding

## Stage 5 — Fix data source (current data is stale at 2023)

Baseball Reference moved current data to dated ZIP archives. The flat CSV URLs still work but are capped at 2023.

- New source: `https://www.baseball-reference.com/data/war_archive-YYYY-MM-DD.zip`
- Each ZIP contains `war_daily_bat.txt` and `war_daily_pitch.txt`
- Update `BrWarDailyLines.kt` to:
  1. Fetch the `/data/` index page via OkHttp
  2. Parse the latest `war_archive-YYYY-MM-DD.zip` filename
  3. Download and unzip it
  4. Cache the extracted `.txt` files as before (7-day expiry)
- Delete stale cached `.txt` files and re-run to confirm fresh 2025 data downloads

## Future — Web UI

See `WEB_BRIEFING.md` for full spec (natural-language → DuckDB SQL, Ktor server, deploy to Fly.io).

---

## Done

- [x] Kotlin 1.9.10 → 2.2.0 (MCU in Stage 3 will take to 2.4.10)
- [x] Gradle 8.3 → 8.14.2
- [x] OkHttp 4.11.0 → 4.12.0
- [x] JUnit Jupiter params 5.10.0 → 5.12.2
- [x] spotless 6.21.0 → 7.0.4
- [x] ben-manes versions 0.48.0 → 0.51.0
