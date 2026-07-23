WITH player_seasons AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        SUM(WAR_def) AS season_dwar,
        MAX(position) AS position
    FROM read_parquet('{{BAT}}')
    WHERE WAR_def IS NOT NULL
    GROUP BY player_ID, year_ID
),
career_stats AS (
    SELECT
        player_ID,
        name_common,
        COUNT(DISTINCT year_ID) AS seasons,
        AVG(season_dwar) AS avg_dwar_per_season,
        MAX(position) AS position
    FROM player_seasons
    GROUP BY player_ID, name_common
    HAVING COUNT(DISTINCT year_ID) >= 5
)
SELECT
    name_common AS player,
    position,
    seasons,
    ROUND(avg_dwar_per_season, 2) AS avg_dwar_per_season
FROM career_stats
ORDER BY avg_dwar_per_season ASC
LIMIT 20
