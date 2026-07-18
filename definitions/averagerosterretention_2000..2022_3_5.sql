WITH all_lines AS (
    SELECT player_id, year_id, LOWER(team_id) AS raw_team, war,
        ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, year_id, LOWER(team_id) AS raw_team, war,
        1000000000 + ROW_NUMBER() OVER () AS source_order
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
player_order AS (
    SELECT player_id, MIN(source_order) AS first_appearance
    FROM all_lines
    GROUP BY player_id
),
eligible_teams AS (
    SELECT raw_team AS team
    FROM all_lines
    GROUP BY raw_team
    HAVING MIN(year_id) <= 2000 AND MAX(year_id) >= 2022
),
player_seasons AS (
    SELECT player_id, year_id, SUM(war) AS season_war
    FROM all_lines
    GROUP BY player_id, year_id
),
roster_memberships AS (
    SELECT DISTINCT
        player_id,
        year_id,
        CASE raw_team
            WHEN 'mon' THEN 'wsn'
            WHEN 'ana' THEN 'laa'
            WHEN 'ath' THEN 'oak'
            ELSE raw_team
        END AS team
    FROM all_lines
),
ranked_rosters AS (
    SELECT r.team, r.year_id, r.player_id,
        ROW_NUMBER() OVER (
            PARTITION BY r.team, r.year_id
            ORDER BY s.season_war DESC, p.first_appearance
        ) AS war_rank
    FROM roster_memberships r
    JOIN player_seasons s USING (player_id, year_id)
    JOIN player_order p USING (player_id)
),
retention_by_year AS (
    SELECT first.team, first.year_id,
        COUNT(last.player_id) AS retained
    FROM ranked_rosters first
    LEFT JOIN roster_memberships last
        ON last.team = first.team
        AND last.player_id = first.player_id
        AND last.year_id = first.year_id + 3
    WHERE first.war_rank <= 5
        AND first.year_id BETWEEN 2000 AND 2019
    GROUP BY first.team, first.year_id
)
SELECT e.team, AVG(r.retained) AS average_years
FROM eligible_teams e
JOIN retention_by_year r USING (team)
WHERE e.team <> 'laa'
GROUP BY e.team
ORDER BY average_years DESC, e.team
