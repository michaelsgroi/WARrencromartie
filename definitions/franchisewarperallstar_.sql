WITH allstar_seasons AS (
    SELECT
        COALESCE(b.team_ID, p.team_ID) AS team_ID,
        a.player_ID,
        a.year_ID,
        COALESCE(b.WAR, 0) + COALESCE(p.WAR, 0) AS season_war
    FROM read_parquet('{{AWARDS}}') a
    LEFT JOIN (
        SELECT player_ID, year_ID, team_ID, SUM(WAR) AS WAR
        FROM read_parquet('{{BAT}}')
        WHERE WAR IS NOT NULL AND lg_ID IN ('AL', 'NL')
        GROUP BY player_ID, year_ID, team_ID
    ) b ON a.player_ID = b.player_ID AND a.year_ID = b.year_ID
    LEFT JOIN (
        SELECT player_ID, year_ID, team_ID, SUM(WAR) AS WAR
        FROM read_parquet('{{PITCH}}')
        WHERE WAR IS NOT NULL AND lg_ID IN ('AL', 'NL')
        GROUP BY player_ID, year_ID, team_ID
    ) p ON a.player_ID = p.player_ID AND a.year_ID = p.year_ID
    WHERE a.award = 'All-Star'
      AND a.winner = TRUE
      AND a.year_ID >= 1933
      AND COALESCE(b.team_ID, p.team_ID) IS NOT NULL
),
team_year_counts AS (
    SELECT team_ID, year_ID, COUNT(*) AS team_allstars_that_year
    FROM allstar_seasons
    GROUP BY team_ID, year_ID
),
non_mandatory AS (
    SELECT s.*
    FROM allstar_seasons s
    JOIN team_year_counts c ON s.team_ID = c.team_ID AND s.year_ID = c.year_ID
    WHERE c.team_allstars_that_year > 1
),
franchise_allstar_stats AS (
    SELECT
        team_ID,
        COUNT(*) AS allstar_appearances,
        AVG(season_war) AS avg_war_in_allstar_seasons
    FROM non_mandatory
    GROUP BY team_ID
)
SELECT
    COALESCE(t.team_name, UPPER(s.team_ID)) AS team,
    s.allstar_appearances,
    ROUND(s.avg_war_in_allstar_seasons, 2) AS avg_war_in_allstar_seasons
FROM franchise_allstar_stats s
LEFT JOIN read_parquet('{{TEAMS}}') t ON UPPER(s.team_ID) = UPPER(t.team_id)
ORDER BY avg_war_in_allstar_seasons ASC
