WITH all_players AS (
    SELECT
        player_ID,
        name_common,
        year_ID,
        WAR,
        SPLIT_PART(position, ',', 1) AS primary_position
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL
      AND position IS NOT NULL
    UNION ALL
    SELECT
        player_ID,
        name_common,
        year_ID,
        WAR,
        SPLIT_PART(positions, ',', 1) AS primary_position
    FROM read_parquet('{{PITCH}}')
    WHERE WAR IS NOT NULL
      AND positions IS NOT NULL
),
season_war AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        primary_position,
        SUM(WAR) AS season_war
    FROM all_players
    WHERE primary_position IS NOT NULL
      AND primary_position != ''
    GROUP BY player_ID, year_ID, primary_position
),
player_position_avg AS (
    SELECT
        player_ID,
        name_common,
        primary_position,
        COUNT(*) AS seasons,
        AVG(season_war) AS avg_war
    FROM season_war
    GROUP BY player_ID, name_common, primary_position
    HAVING COUNT(*) >= 10
),
ranked AS (
    SELECT
        *,
        ROW_NUMBER() OVER (PARTITION BY primary_position ORDER BY avg_war ASC) AS rn
    FROM player_position_avg
)
SELECT
    primary_position AS position,
    name_common AS player,
    seasons,
    ROUND(avg_war, 2) AS avg_season_war
FROM ranked
WHERE rn = 1
ORDER BY
    CASE primary_position
        WHEN 'C'  THEN 1
        WHEN '1B' THEN 2
        WHEN '2B' THEN 3
        WHEN '3B' THEN 4
        WHEN 'SS' THEN 5
        WHEN 'LF' THEN 6
        WHEN 'CF' THEN 7
        WHEN 'RF' THEN 8
        WHEN 'OF' THEN 9
        WHEN 'DH' THEN 10
        WHEN 'P'  THEN 11
        ELSE 12
    END
