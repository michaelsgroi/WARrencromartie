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
    SELECT player_id, year_id, SUM(war) AS season_war, MIN(source_order) AS season_order
    FROM all_lines GROUP BY player_id, year_id
),
careers AS (
    SELECT l.player_id, FIRST(l.name_common ORDER BY l.source_order) AS name,
        SUM(l.war) AS career_war, COUNT(DISTINCT l.year_id) AS season_count,
        MIN(l.year_id) AS min_year, MAX(l.year_id) AS max_year,
        MIN(l.source_order) AS player_order
    FROM all_lines l GROUP BY l.player_id
),
peaks AS (
    SELECT player_id, year_id AS peak_year, season_war AS peak_war,
        ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY season_war DESC, season_order) AS peak_rank
    FROM seasons
),
teams AS (
    SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM (
        SELECT player_id, team_id, MIN(source_order) AS first_appearance
        FROM all_lines GROUP BY player_id, team_id
    ) t GROUP BY player_id
),
selected AS (
    SELECT s.player_id, s.season_war, s.season_order
    FROM seasons s JOIN careers c USING (player_id)
    WHERE c.career_war < 15
    ORDER BY s.season_war DESC, s.season_order
    LIMIT 20
)
SELECT c.name, ROUND(c.career_war, 2) AS career_war,
    CAST(ROUND(p.peak_war, 2) AS VARCHAR) || ' (' || p.peak_year || ')' AS peak_war,
    c.season_count, '(' || c.min_year || '-' || c.max_year || ')' AS year_range, t.teams
FROM selected s
JOIN careers c USING (player_id)
JOIN peaks p ON p.player_id = s.player_id AND p.peak_rank = 1
JOIN teams t USING (player_id)
ORDER BY s.season_war DESC, s.season_order
