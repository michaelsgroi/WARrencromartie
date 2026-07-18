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
        ROUND(SUM(war), 2) AS career_war,
        COUNT(DISTINCT year_id) AS season_count,
        MIN(year_id) AS min_year, MAX(year_id) AS max_year
    FROM all_seasons GROUP BY player_id
),
rows_numbered AS (
    SELECT player_id, UPPER(team_id) AS team_id, ROW_NUMBER() OVER () AS rn
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, UPPER(team_id) AS team_id, ROW_NUMBER() OVER () AS rn
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
player_teams AS (
    SELECT player_id, team_id, MIN(rn) AS first_appearance
    FROM rows_numbered GROUP BY player_id, team_id
),
teams AS (
    SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM player_teams GROUP BY player_id
)
SELECT c.name, c.career_war, c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range, t.teams
FROM careers c JOIN teams t USING (player_id)
WHERE c.player_id IN (
    SELECT player_id FROM all_seasons
    WHERE year_id = 1996 AND team_id = 'CLE'
)
ORDER BY ROUND(c.career_war, 2) DESC, c.name
