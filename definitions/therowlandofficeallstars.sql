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
        LIST_REDUCE(LIST(war ORDER BY source_order), (subtotal, value) -> subtotal + value) AS season_war,
        MIN(source_order) AS season_order
    FROM all_lines
    GROUP BY player_id, year_id
),
career_totals AS (
    SELECT player_id,
        LIST_REDUCE(LIST(season_war ORDER BY season_order), (subtotal, value) -> subtotal + value) AS career_war,
        COUNT(*) AS season_count,
        MIN(year_id) AS min_year,
        MAX(year_id) AS max_year
    FROM player_seasons
    GROUP BY player_id
),
career_identity AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name,
        MIN(source_order) AS career_order
    FROM all_lines
    GROUP BY player_id
),
careers AS (
    SELECT * FROM career_totals JOIN career_identity USING (player_id)
),
teams AS (
    SELECT player_id,
        STRING_AGG(team_id, ',' ORDER BY season_appearance, team_appearance) AS teams
    FROM (
        SELECT player_id, team_id,
            MIN(season_appearance) AS season_appearance,
            MIN(source_order) AS team_appearance
        FROM (
            SELECT player_id, year_id, team_id, source_order,
                MIN(source_order) OVER (PARTITION BY player_id, year_id) AS season_appearance
            FROM all_lines
        ) season_lines
        GROUP BY player_id, team_id
    )
    GROUP BY player_id
)
SELECT c.name, ROUND(c.career_war, 2) AS career_war, c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range,
    t.teams
FROM careers c JOIN teams t USING (player_id)
WHERE c.season_count >= 10 AND c.career_war < 0.0
ORDER BY c.career_war, c.career_order
