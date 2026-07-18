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
player_info AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name,
        MIN(source_order) AS player_order
    FROM all_lines
    GROUP BY player_id
),
seasons AS (
    SELECT player_id, year_id, SUM(war) AS season_war,
        MIN(source_order) AS season_order
    FROM all_lines
    GROUP BY player_id, year_id
),
ranked_seasons AS (
    SELECT *, ROW_NUMBER() OVER (
        PARTITION BY player_id
        ORDER BY season_war DESC, season_order
    ) AS season_rank
    FROM seasons
),
selected_seasons AS (
    SELECT * FROM ranked_seasons WHERE season_rank <= 3
),
top_careers AS (
    SELECT s.player_id, p.name, p.player_order,
        SUM(s.season_war) AS top_war,
        COUNT(*) AS season_count,
        MIN(s.year_id) AS min_year,
        MAX(s.year_id) AS max_year
    FROM selected_seasons s
    JOIN player_info p USING (player_id)
    GROUP BY s.player_id, p.name, p.player_order
),
selected_team_appearances AS (
    SELECT l.player_id, l.team_id, MIN(l.source_order) AS first_appearance
    FROM all_lines l
    JOIN selected_seasons s
      ON l.player_id = s.player_id AND l.year_id = s.year_id
    GROUP BY l.player_id, l.team_id
),
teams AS (
    SELECT player_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM selected_team_appearances
    GROUP BY player_id
)
SELECT c.name, ROUND(c.top_war, 2) AS career_war, c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range,
    t.teams
FROM top_careers c
JOIN teams t USING (player_id)
ORDER BY c.top_war DESC, c.player_order
LIMIT 50
