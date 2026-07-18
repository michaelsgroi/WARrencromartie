WITH all_lines AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
careers AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name,
        ROUND(SUM(war), 2) AS career_war, COUNT(DISTINCT year_id) AS season_count,
        MIN(year_id) AS min_year, MAX(year_id) AS max_year,
        MIN(source_order) AS player_order
    FROM all_lines GROUP BY player_id
),
teams AS (
    SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM (
        SELECT player_id, team_id, MIN(source_order) AS first_appearance
        FROM all_lines GROUP BY player_id, team_id
    ) t GROUP BY player_id
)
SELECT c.name, c.career_war, c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range, t.teams
FROM careers c JOIN teams t USING (player_id)
WHERE LOWER(c.name) LIKE 'babe %'
ORDER BY c.career_war DESC, c.player_order
