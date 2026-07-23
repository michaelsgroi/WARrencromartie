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
player_stats AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        COUNT(DISTINCT year_ID) AS seasons,
        ROUND(AVG(season_dwar), 3) AS avg_dwar_per_season,
        ROUND(SUM(season_dwar), 1) AS career_dwar,
        STRING_AGG(DISTINCT position, ', ') AS positions_played
    FROM player_seasons
    WHERE position IS NOT NULL
    GROUP BY player_ID
    HAVING COUNT(DISTINCT year_ID) >= 5
)
SELECT
    name_common AS player,
    seasons,
    avg_dwar_per_season,
    career_dwar,
    positions_played
FROM player_stats
ORDER BY avg_dwar_per_season DESC
LIMIT 20
