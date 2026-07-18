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
player_seasons AS (
    SELECT player_id, year_id,
        LIST_REDUCE(LIST(war ORDER BY source_order), (x, y) -> x + y) AS season_war,
        MIN(source_order) AS season_first_appearance
    FROM all_lines
    GROUP BY player_id, year_id
),
player_metadata AS (
    SELECT player_id,
        FIRST(name_common ORDER BY source_order) AS name,
        MIN(source_order) AS first_appearance
    FROM all_lines
    GROUP BY player_id
),
careers AS (
    SELECT s.player_id, m.name,
        LIST_REDUCE(
            LIST(s.season_war ORDER BY s.season_first_appearance),
            (x, y) -> x + y
        ) AS career_war,
        COUNT(*) AS season_count,
        MIN(s.year_id) AS min_year,
        MAX(s.year_id) AS max_year,
        m.first_appearance
    FROM player_seasons s
    JOIN player_metadata m USING (player_id)
    GROUP BY s.player_id, m.name, m.first_appearance
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
    FROM rows_numbered
    GROUP BY player_id, team_id
),
teams AS (
    SELECT player_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams,
        BOOL_OR(team_id = UPPER('bos')) AS played_for_team
    FROM player_teams
    GROUP BY player_id
)
SELECT c.name,
    CASE WHEN ROUND(c.career_war, 2) = 0 THEN 0 ELSE ROUND(c.career_war, 2) END AS career_war,
    c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range,
    t.teams
FROM careers c
JOIN teams t USING (player_id)
WHERE t.played_for_team
ORDER BY c.career_war ASC, c.first_appearance
LIMIT 30
