WITH all_lines AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        COALESCE(salary, 0) AS salary, ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        COALESCE(salary, 0) AS salary, 1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
seasons AS (
    SELECT player_id, year_id, SUM(war) AS season_war,
        CAST(ROUND(AVG(salary)) AS BIGINT) AS season_salary,
        MIN(source_order) AS season_order
    FROM all_lines GROUP BY player_id, year_id
),
season_teams AS (
    SELECT player_id, year_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM (
        SELECT player_id, year_id, team_id, MIN(source_order) AS first_appearance
        FROM all_lines GROUP BY player_id, year_id, team_id
    ) t GROUP BY player_id, year_id
),
player_info AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name
    FROM all_lines GROUP BY player_id
)
SELECT p.name, ROUND(s.season_war, 2) AS war, s.season_salary AS salary,
    s.year_id AS year, t.teams
FROM seasons s
JOIN player_info p USING (player_id)
JOIN season_teams t USING (player_id, year_id)
WHERE s.season_salary > 0 AND s.year_id >= 1947
ORDER BY s.season_war / s.season_salary DESC, s.season_order
LIMIT 20

