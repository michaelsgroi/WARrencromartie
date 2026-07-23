WITH player_seasons AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        MAX(year_ID) AS max_year,
        COUNT(DISTINCT year_ID) AS seasons_played,
        SUM(WAR) AS career_war
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL
    GROUP BY player_ID
    UNION ALL
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        MAX(year_ID) AS max_year,
        COUNT(DISTINCT year_ID) AS seasons_played,
        SUM(WAR) AS career_war
    FROM read_parquet('{{PITCH}}')
    WHERE WAR IS NOT NULL
    GROUP BY player_ID
),
combined AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        MAX(max_year) AS max_year,
        SUM(seasons_played) AS total_seasons,
        SUM(career_war) AS career_war
    FROM player_seasons
    GROUP BY player_ID
),
max_year_ref AS (
    SELECT MAX(year_ID) AS current_year FROM read_parquet('{{BAT}}')
)
SELECT
    name_common AS player,
    total_seasons AS seasons_played,
    ROUND(career_war, 1) AS career_war,
    max_year AS last_season
FROM combined
CROSS JOIN max_year_ref
WHERE max_year >= current_year
  AND total_seasons >= 8
ORDER BY career_war ASC
LIMIT 10
