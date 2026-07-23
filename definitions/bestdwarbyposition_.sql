WITH player_seasons AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        SUM(WAR_def) AS season_dwar,
        MAX(position) AS position
    FROM read_parquet('{{BAT}}')
    WHERE WAR_def IS NOT NULL
      AND position IS NOT NULL
    GROUP BY player_ID, year_ID
),
player_primary_position AS (
    SELECT
        player_ID,
        name_common,
        year_ID,
        season_dwar,
        TRIM(SPLIT_PART(position, ',', 1)) AS primary_position
    FROM player_seasons
),
player_pos_stats AS (
    SELECT
        player_ID,
        name_common,
        primary_position,
        COUNT(DISTINCT year_ID) AS seasons,
        AVG(season_dwar) AS avg_dwar_per_season
    FROM player_primary_position
    GROUP BY player_ID, name_common, primary_position
    HAVING COUNT(DISTINCT year_ID) >= 10
),
ranked AS (
    SELECT
        primary_position AS position,
        name_common AS player,
        seasons,
        ROUND(avg_dwar_per_season, 2) AS avg_dwar_per_season,
        ROW_NUMBER() OVER (PARTITION BY primary_position ORDER BY avg_dwar_per_season DESC) AS rn
    FROM player_pos_stats
    WHERE primary_position IN ('C','1B','2B','3B','SS','LF','CF','RF','OF','DH')
)
SELECT position, player, seasons, avg_dwar_per_season
FROM ranked
WHERE rn = 1
ORDER BY position
