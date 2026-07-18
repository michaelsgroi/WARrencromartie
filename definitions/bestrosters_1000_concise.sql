WITH all_lines AS (
    SELECT player_id, year_id, LOWER(team_id) AS team_id, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, year_id, LOWER(team_id) AS team_id, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
player_seasons AS (
    SELECT player_id, year_id,
        LIST_REDUCE(LIST(war ORDER BY source_order), (x, y) -> x + y) AS season_war,
        MIN(source_order) AS season_order
    FROM all_lines
    GROUP BY player_id, year_id
),
careers AS (
    SELECT player_id,
        LIST_REDUCE(
            LIST(season_war ORDER BY season_order),
            (x, y) -> x + y
        ) AS career_war,
        MIN(season_order) AS player_order
    FROM player_seasons
    GROUP BY player_id
),
memberships AS (
    SELECT player_id, year_id, team_id, MIN(source_order) AS membership_order
    FROM all_lines
    GROUP BY player_id, year_id, team_id
),
rosters AS (
    SELECT m.year_id, m.team_id,
        LIST_REDUCE(
            LIST(c.career_war ORDER BY c.player_order),
            (x, y) -> x + y
        ) AS roster_war,
        MIN(STRUCT_PACK(
            player_order := c.player_order,
            membership_order := m.membership_order
        )) AS roster_order
    FROM memberships m
    JOIN careers c USING (player_id)
    GROUP BY m.year_id, m.team_id
),
ranked AS (
    SELECT *, ROW_NUMBER() OVER (ORDER BY roster_war DESC, roster_order) AS report_rank
    FROM rosters
)
SELECT CAST(report_rank AS VARCHAR) || ':' AS rank_label,
    year_id,
    team_id,
    CAST(ROUND(roster_war) AS BIGINT) AS roster_war
FROM ranked
ORDER BY report_rank
LIMIT 1000
