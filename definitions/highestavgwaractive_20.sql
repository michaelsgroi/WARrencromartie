WITH player_seasons AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        SUM(WAR) AS season_war
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL
      AND lg_ID IN ('AL', 'NL')
    GROUP BY player_ID, year_ID
    UNION ALL
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        SUM(WAR) AS season_war
    FROM read_parquet('{{PITCH}}')
    WHERE WAR IS NOT NULL
      AND lg_ID IN ('AL', 'NL')
    GROUP BY player_ID, year_ID
),
combined AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        SUM(season_war) AS season_war
    FROM player_seasons
    GROUP BY player_ID, year_ID
),
max_year AS (
    SELECT MAX(year_ID) AS latest FROM read_parquet('{{BAT}}')
),
active_players AS (
    SELECT player_ID
    FROM combined
    GROUP BY player_ID
    HAVING MAX(year_ID) >= (SELECT latest FROM max_year)
),
career_avg AS (
    SELECT
        c.player_ID,
        MAX(c.name_common) AS name_common,
        COUNT(DISTINCT c.year_ID) AS seasons,
        ROUND(AVG(c.season_war), 2) AS avg_war_per_season,
        ROUND(SUM(c.season_war), 1) AS career_war
    FROM combined c
    INNER JOIN active_players a ON c.player_ID = a.player_ID
    GROUP BY c.player_ID
    HAVING COUNT(DISTINCT c.year_ID) >= 3
)
SELECT
    name_common AS player,
    seasons,
    avg_war_per_season,
    career_war
FROM career_avg
ORDER BY avg_war_per_season DESC
LIMIT 20
