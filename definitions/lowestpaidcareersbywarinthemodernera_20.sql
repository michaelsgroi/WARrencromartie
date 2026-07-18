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
careers AS (
    SELECT s.player_id, p.name,
        SUM(s.season_war) AS career_war, SUM(s.season_salary) AS career_salary,
        COUNT(*) AS season_count, MIN(s.year_id) AS min_year, MAX(s.year_id) AS max_year,
        MIN(s.season_order) AS player_order
    FROM seasons s
    JOIN (
        SELECT player_id, FIRST(name_common ORDER BY source_order) AS name
        FROM all_lines GROUP BY player_id
    ) p USING (player_id)
    GROUP BY s.player_id, p.name
),
teams AS (
    SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM (
        SELECT player_id, team_id, MIN(source_order) AS first_appearance
        FROM all_lines GROUP BY player_id, team_id
    ) t GROUP BY player_id
)
SELECT c.name, ROUND(c.career_war, 2) AS career_war, c.career_salary,
    c.season_count, '(' || c.min_year || '-' || c.max_year || ')' AS year_range, t.teams
FROM careers c JOIN teams t USING (player_id)
WHERE c.career_salary > 0 AND c.max_year >= 1947
ORDER BY c.career_war / c.career_salary DESC, c.player_order
LIMIT 20
