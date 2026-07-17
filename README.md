A Kotlin CLI tool that downloads [WAR (Wins Above Replacement)](https://www.reddit.com/r/baseball/comments/26fd78/how_to_download_raw_data_for_war_from/) data from Baseball Reference and generates 80+ statistical reports as text files.

## How it works

1. Downloads two CSV files from dated ZIP archives at [baseball-reference.com/data/](https://www.baseball-reference.com/data/) (auto-refreshed every 7 days):
   - `war_daily_bat.txt` — batting seasons
   - `war_daily_pitch.txt` — pitching seasons
   
   Note: the flat CSV files at that URL are stale (capped at 2023); the dated ZIP archives (`war_archive-YYYY-MM-DD.zip`) contain current data through the present season.
2. Merges batting and pitching into a unified career/season/roster model
3. Generates reports to the [reports](reports) directory

## Key classes

| Class | Role |
|---|---|
| [`BrWarDailyLines`](src/main/kotlin/com/michaelsgroi/baseballreference/BrWarDailyLines.kt) | Fetches and parses one CSV file via OkHttp |
| [`BrWarDaily`](src/main/kotlin/com/michaelsgroi/baseballreference/BrWarDaily.kt) | Merges batting + pitching into `seasons`, `careers`, `rosters` |
| `Career` / `Season` / `Roster` | Domain model |
| [`BrReports`](src/main/kotlin/com/michaelsgroi/baseballreference/BrReports.kt) | ~80 named report methods; auto-derives output filenames via `StackWalker` |
| [`BrReportFormatter`](src/main/kotlin/com/michaelsgroi/baseballreference/BrReportFormatter.kt) | Generic tabular formatter |

## Reports

Examples of what's generated:

- Career WAR leaders (all-time, by team, batting, pitching)
- Best/worst single-season WARs
- Salary efficiency (best and worst $/WAR, careers and seasons)
- Consecutive seasons above a WAR threshold
- Team rosters and season breakdowns
- Individual player career breakdowns (e.g. Babe Ruth)
- Franchise roster retention analysis

## Usage

```
make        # regenerate all reports
make test   # run tests
```

Tests use real data and assert specific values for historical players (Babe Ruth career WAR 182.55, etc.).
