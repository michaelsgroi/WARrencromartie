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
seasons AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name, year_id,
        SUM(war) AS season_war, MIN(source_order) AS season_order
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
oak_2019_ranked AS (
    SELECT s.player_id,
        ROW_NUMBER() OVER (ORDER BY s.season_war DESC, s.season_order) - 1 AS war_index
    FROM seasons s
    WHERE s.year_id = 2019
      AND EXISTS (SELECT 1 FROM all_lines l
                  WHERE l.player_id = s.player_id AND l.year_id = 2019 AND l.team_id = 'OAK')
),
retained_indexes AS (
    SELECT o.war_index
    FROM oak_2019_ranked o
    WHERE EXISTS (SELECT 1 FROM all_lines l
                  WHERE l.player_id = o.player_id AND l.year_id = 2022 AND l.team_id = 'OAK')
),
target_ranked AS (
    SELECT s.*,
        ROW_NUMBER() OVER (ORDER BY s.season_war DESC, s.player_id, s.season_order) - 1 AS war_index
    FROM seasons s
    WHERE s.year_id = 2019
      AND EXISTS (SELECT 1 FROM all_lines l
                  WHERE l.player_id = s.player_id AND l.year_id = 2019 AND l.team_id = 'BOS')
)
SELECT t.name, ROUND(t.season_war, 2) AS war, t.year_id AS "year", st.teams
FROM target_ranked t
JOIN retained_indexes r USING (war_index)
JOIN season_teams st USING (player_id, year_id)
ORDER BY t.season_war DESC, t.season_order
