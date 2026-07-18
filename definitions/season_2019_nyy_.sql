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
roster_players AS (
    SELECT DISTINCT player_id
    FROM all_lines
    WHERE year_id = 2019 AND team_id = 'NYY'
),
player_order AS (
    SELECT player_id, MIN(source_order) AS first_appearance
    FROM all_lines
    GROUP BY player_id
),
season_totals AS (
    SELECT player_id, year_id,
        FIRST(name_common ORDER BY source_order) AS name,
        SUM(war) AS raw_war
    FROM all_lines
    WHERE year_id = 2019
    GROUP BY player_id, year_id
),
player_teams AS (
    SELECT player_id, year_id, team_id, MIN(source_order) AS first_appearance
    FROM all_lines
    WHERE year_id = 2019
    GROUP BY player_id, year_id, team_id
),
teams AS (
    SELECT player_id, year_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM player_teams
    GROUP BY player_id, year_id
)
SELECT s.name, ROUND(s.raw_war, 2) AS war, s.year_id AS year, t.teams
FROM roster_players r
JOIN season_totals s USING (player_id)
JOIN teams t USING (player_id, year_id)
JOIN player_order p USING (player_id)
ORDER BY s.raw_war DESC, p.first_appearance
