WITH latest_year AS (
    SELECT MAX(year_ID) AS max_year FROM read_parquet('{{BAT}}')
),
player_seasons AS (
    SELECT
        b.player_ID,
        MAX(b.name_common) AS name_common,
        COUNT(DISTINCT b.year_ID) AS seasons_played,
        SUM(COALESCE(b.WAR, 0)) + SUM(COALESCE(p.WAR, 0)) AS total_war,
        MAX(b.year_ID) AS last_season
    FROM read_parquet('{{BAT}}') b
    LEFT JOIN read_parquet('{{PITCH}}') p ON b.player_ID = p.player_ID AND b.year_ID = p.year_ID
    WHERE b.WAR IS NOT NULL
    GROUP BY b.player_ID
),
active_players AS (
    SELECT *
    FROM player_seasons
    CROSS JOIN latest_year
    WHERE last_season >= latest_year.max_year
      AND seasons_played >= 8
)
SELECT
    name_common AS player,
    seasons_played,
    ROUND(total_war / seasons_played, 2) AS avg_war_per_season,
    ROUND(total_war, 2) AS total_war
FROM active_players
ORDER BY avg_war_per_season ASC
LIMIT 10
