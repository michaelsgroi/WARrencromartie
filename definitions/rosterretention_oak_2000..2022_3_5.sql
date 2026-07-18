WITH all_lines AS (
    SELECT player_id, year_id, LOWER(team_id) AS team, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, year_id, LOWER(team_id) AS team, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
player_order AS (
    SELECT player_id, MIN(source_order) AS first_appearance
    FROM all_lines
    GROUP BY player_id
),
player_seasons AS (
    SELECT player_id, year_id, SUM(war) AS season_war
    FROM all_lines
    GROUP BY player_id, year_id
),
roster_memberships AS (
    SELECT DISTINCT player_id, year_id, team
    FROM all_lines
),
ranked_rosters AS (
    SELECT r.year_id, r.player_id,
        ROW_NUMBER() OVER (
            PARTITION BY r.year_id
            ORDER BY s.season_war DESC, p.first_appearance
        ) AS war_rank
    FROM roster_memberships r
    JOIN player_seasons s USING (player_id, year_id)
    JOIN player_order p USING (player_id)
    WHERE r.team = 'oak'
)
SELECT first.year_id AS "1", first.year_id + 3 AS "2",
    COUNT(last.player_id) AS "3"
FROM ranked_rosters first
LEFT JOIN roster_memberships last
    ON last.team = 'oak'
    AND last.player_id = first.player_id
    AND last.year_id = first.year_id + 3
WHERE first.war_rank <= 5
    AND first.year_id BETWEEN 2000 AND 2019
GROUP BY first.year_id
ORDER BY first.year_id
