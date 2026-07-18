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
careers AS (
    SELECT player_id, SUM(war) AS career_war
    FROM all_lines
    GROUP BY player_id
),
memberships AS (
    SELECT player_id, year_id, team_id, MIN(source_order) AS membership_order
    FROM all_lines
    GROUP BY player_id, year_id, team_id
),
rosters AS (
    SELECT m.year_id, m.team_id, SUM(c.career_war) AS roster_war,
        MIN(m.membership_order) AS roster_order
    FROM memberships m
    JOIN careers c USING (player_id)
    GROUP BY m.year_id, m.team_id
),
ranked AS (
    SELECT *, ROW_NUMBER() OVER (
        PARTITION BY team_id
        ORDER BY roster_war DESC, roster_order
    ) AS team_rank
    FROM rosters
)
SELECT CAST(ROW_NUMBER() OVER (ORDER BY roster_war DESC, roster_order) AS VARCHAR) || ':' AS rank_label,
    year_id, team_id, CAST(ROUND(roster_war) AS BIGINT) AS displayed_war
FROM ranked
WHERE team_rank = 1
ORDER BY roster_war DESC, roster_order
