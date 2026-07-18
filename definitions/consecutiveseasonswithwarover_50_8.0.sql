WITH all_lines AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}') WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}') WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
seasons AS (
    SELECT player_id, year_id, SUM(war) AS season_war, MIN(source_order) AS season_order
    FROM all_lines GROUP BY player_id, year_id
),
ordered AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY season_order) AS ordinal
    FROM seasons
),
qualifying AS (
    SELECT *, ordinal - ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY ordinal) AS streak_id
    FROM ordered WHERE season_war > 8.0
),
streaks AS (
    SELECT player_id, streak_id, COUNT(*) AS streak_length, SUM(season_war) AS streak_war
    FROM qualifying GROUP BY player_id, streak_id
),
chosen AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY streak_length DESC, streak_war DESC) AS choice
    FROM streaks
),
selected AS (
    SELECT q.* FROM qualifying q JOIN chosen c USING (player_id, streak_id) WHERE c.choice = 1
),
player_info AS (
    SELECT player_id, FIRST(name_common ORDER BY source_order) AS name, MIN(source_order) AS player_order
    FROM all_lines GROUP BY player_id
),
summary AS (
    SELECT s.player_id, p.name, COUNT(*) AS season_count, MIN(s.year_id) AS min_year,
        MAX(s.year_id) AS max_year, p.player_order
    FROM selected s JOIN player_info p USING (player_id)
    GROUP BY s.player_id, p.name, p.player_order
),
teams AS (
    SELECT s.player_id, STRING_AGG(s.team_id, ',' ORDER BY s.first_appearance) AS teams
    FROM (
        SELECT x.player_id, l.team_id, MIN(l.source_order) AS first_appearance
        FROM selected x JOIN all_lines l ON l.player_id=x.player_id AND l.year_id=x.year_id
        GROUP BY x.player_id, l.team_id
    ) s GROUP BY s.player_id
)
SELECT x.name, x.season_count, '(' || x.min_year || '-' || x.max_year || ')' AS year_range, t.teams
FROM summary x JOIN teams t USING (player_id)
ORDER BY x.season_count DESC, x.player_order LIMIT 50
