WITH latest_year AS (
    SELECT MAX(year_ID) AS max_year FROM read_parquet('{{BAT}}')
),
player_max_year AS (
    SELECT player_ID, MAX(year_ID) AS last_year
    FROM (
        SELECT player_ID, year_ID FROM read_parquet('{{BAT}}') WHERE WAR IS NOT NULL
        UNION ALL
        SELECT player_ID, year_ID FROM read_parquet('{{PITCH}}') WHERE WAR IS NOT NULL
    ) combined
    GROUP BY player_ID
),
active_players AS (
    SELECT player_ID
    FROM player_max_year, latest_year
    WHERE last_year >= max_year
),
career_war AS (
    SELECT
        player_ID,
        SUM(WAR) AS total_war
    FROM (
        SELECT player_ID, WAR FROM read_parquet('{{BAT}}') WHERE WAR IS NOT NULL
        UNION ALL
        SELECT player_ID, WAR FROM read_parquet('{{PITCH}}') WHERE WAR IS NOT NULL
    ) all_war
    WHERE player_ID IN (SELECT player_ID FROM active_players)
    GROUP BY player_ID
),
names AS (
    SELECT player_ID, MAX(name_common) AS name_common
    FROM (
        SELECT player_ID, name_common FROM read_parquet('{{BAT}}')
        UNION ALL
        SELECT player_ID, name_common FROM read_parquet('{{PITCH}}')
    ) n
    GROUP BY player_ID
)
SELECT
    name_common AS player,
    ROUND(cw.total_war, 1) AS career_war
FROM career_war cw
JOIN names n ON cw.player_ID = n.player_ID
ORDER BY career_war DESC
LIMIT 20
