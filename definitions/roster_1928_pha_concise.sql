WITH all_seasons AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
careers AS (
    SELECT player_id, FIRST(name_common ORDER BY year_id) AS name,
        SUM(war) AS career_war
    FROM all_seasons
    GROUP BY player_id
)
SELECT c.name, CAST(ROUND(c.career_war) AS BIGINT) AS war
FROM careers c
WHERE c.player_id IN (
    SELECT player_id FROM all_seasons
    WHERE year_id = 1928 AND team_id = 'PHA'
)
ORDER BY c.career_war DESC, c.name
