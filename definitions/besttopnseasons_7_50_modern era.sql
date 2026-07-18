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
eligible_players AS (
    SELECT player_id
    FROM player_seasons
    GROUP BY player_id
    HAVING MAX(year_id) >= 1947
),
ranked_seasons AS (
    SELECT s.*,
        ROW_NUMBER() OVER (
            PARTITION BY s.player_id
            ORDER BY s.season_war DESC, s.season_first_appearance
        ) AS season_rank
    FROM player_seasons s
    JOIN eligible_players e USING (player_id)
),
selected_seasons AS (
    SELECT *
    FROM ranked_seasons
    WHERE season_rank <= 7
),
player_metadata AS (
    SELECT player_id,
        FIRST(name_common ORDER BY source_order) AS name,
        MIN(source_order) AS first_appearance
    FROM all_lines
    GROUP BY player_id
),
peaks AS (
    SELECT s.player_id, m.name,
        LIST_REDUCE(
            LIST(s.season_war ORDER BY s.season_first_appearance),
            (x, y) -> x + y
        ) AS peak_war,
        COUNT(*) AS season_count,
        MIN(s.year_id) AS min_year,
        MAX(s.year_id) AS max_year,
        m.first_appearance
    FROM selected_seasons s
    JOIN player_metadata m USING (player_id)
    GROUP BY s.player_id, m.name, m.first_appearance
),
selected_lines AS (
    SELECT l.*
    FROM all_lines l
    JOIN selected_seasons s USING (player_id, year_id)
),
player_teams AS (
    SELECT player_id, team_id, MIN(source_order) AS first_appearance
    FROM selected_lines
    GROUP BY player_id, team_id
),
teams AS (
    SELECT player_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM player_teams
    GROUP BY player_id
)
SELECT p.name,
    CASE WHEN ROUND(p.peak_war, 2) = 0 THEN 0 ELSE ROUND(p.peak_war, 2) END AS career_war,
    p.season_count,
    '(' || p.min_year || '-' || p.max_year || ')' AS year_range,
    t.teams
FROM peaks p
JOIN teams t USING (player_id)
ORDER BY p.peak_war DESC, p.first_appearance
LIMIT 50
