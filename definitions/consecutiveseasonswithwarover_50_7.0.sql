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
ordered_seasons AS (
    SELECT *, ROW_NUMBER() OVER (
        PARTITION BY player_id ORDER BY season_order
    ) AS season_ordinal
    FROM seasons
),
qualifying AS (
    SELECT *, season_ordinal - ROW_NUMBER() OVER (
        PARTITION BY player_id ORDER BY season_ordinal
    ) AS streak_group
    FROM ordered_seasons
    WHERE season_war > 7.0
),
streaks AS (
    SELECT player_id, streak_group, COUNT(*) AS season_count,
        SUM(season_war) AS streak_war, MIN(year_id) AS min_year,
        MAX(year_id) AS max_year, MIN(season_order) AS streak_order
    FROM qualifying
    GROUP BY player_id, streak_group
),
best_streaks AS (
    SELECT *, ROW_NUMBER() OVER (
        PARTITION BY player_id
        ORDER BY season_count DESC, streak_war DESC, streak_order
    ) AS streak_rank
    FROM streaks
),
top_careers AS (
    SELECT p.player_id, p.name, p.player_order, b.streak_group,
        COALESCE(b.season_count, 0) AS season_count, b.streak_war,
        b.min_year, b.max_year, b.streak_order
    FROM player_info p
    LEFT JOIN best_streaks b
        ON p.player_id = b.player_id AND b.streak_rank = 1
    ORDER BY COALESCE(b.season_count, 0) DESC, p.player_order
    LIMIT 50
),
team_appearances AS (
    SELECT l.player_id, l.team_id, MIN(l.source_order) AS first_appearance
    FROM all_lines l
    JOIN top_careers c ON l.player_id = c.player_id
    JOIN qualifying q ON q.player_id = c.player_id
        AND q.streak_group = c.streak_group
        AND q.year_id = l.year_id
    GROUP BY l.player_id, l.team_id
),
teams AS (
    SELECT player_id,
        STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
    FROM team_appearances
    GROUP BY player_id
)
SELECT c.name, c.season_count,
    '(' || c.min_year || '-' || c.max_year || ')' AS year_range,
    t.teams
FROM top_careers c
LEFT JOIN teams t USING (player_id)
ORDER BY c.season_count DESC, c.player_order
