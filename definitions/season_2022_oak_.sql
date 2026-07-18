WITH all_lines AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL') AND year_id = 2022
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL') AND year_id = 2022
),
roster_players AS (
    SELECT DISTINCT player_id FROM all_lines WHERE team_id = 'OAK'
),
seasons AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name,
        ROUND(SUM(war), 2) AS season_war, MIN(source_order) AS season_order
    FROM all_lines
    WHERE player_id IN (SELECT player_id FROM roster_players)
    GROUP BY player_id
),
teams AS (
    SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM (
        SELECT player_id, team_id, MIN(source_order) AS first_appearance
        FROM all_lines
        WHERE player_id IN (SELECT player_id FROM roster_players)
        GROUP BY player_id, team_id
    )
    GROUP BY player_id
)
SELECT s.name, s.season_war, 2022 AS year_id, t.teams
FROM seasons s JOIN teams t USING (player_id)
ORDER BY s.season_war DESC, s.season_order
