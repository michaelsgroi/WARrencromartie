WITH cy_young_winners AS (
    SELECT
        a.year_ID,
        a.lg_ID,
        a.player_ID AS winner_player_ID,
        SUM(p.WAR) AS winner_war
    FROM read_parquet('{{AWARDS}}') a
    JOIN read_parquet('{{PITCH}}') p ON a.player_ID = p.player_ID AND a.year_ID = p.year_ID
    WHERE a.award = 'Cy Young Award'
      AND a.winner = TRUE
      AND p.WAR IS NOT NULL
    GROUP BY a.year_ID, a.lg_ID, a.player_ID
),
cy_young_league_mapped AS (
    SELECT
        year_ID,
        winner_player_ID,
        winner_war,
        CASE WHEN lg_ID = 'ML' THEN NULL ELSE lg_ID END AS award_lg
    FROM cy_young_winners
),
pitcher_seasons AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        year_ID,
        lg_ID,
        SUM(WAR) AS pitcher_war
    FROM read_parquet('{{PITCH}}')
    WHERE WAR IS NOT NULL
      AND year_ID >= 1956
      AND lg_ID IN ('AL', 'NL')
    GROUP BY player_ID, year_ID, lg_ID
),
winner_names AS (
    SELECT DISTINCT player_ID, MAX(name_common) AS name_common
    FROM read_parquet('{{PITCH}}')
    GROUP BY player_ID
),
snubs AS (
    SELECT
        ps.player_ID,
        ps.name_common,
        ps.year_ID,
        ps.lg_ID,
        ps.pitcher_war,
        cyw.winner_player_ID,
        cyw.winner_war,
        ps.pitcher_war - cyw.winner_war AS war_diff
    FROM pitcher_seasons ps
    JOIN cy_young_league_mapped cyw
        ON ps.year_ID = cyw.year_ID
        AND (cyw.award_lg IS NULL OR ps.lg_ID = cyw.award_lg)
    WHERE ps.player_ID != cyw.winner_player_ID
      AND ps.pitcher_war > cyw.winner_war
      AND NOT EXISTS (
          SELECT 1 FROM read_parquet('{{AWARDS}}') la
          WHERE la.player_ID = ps.player_ID
            AND la.year_ID = ps.year_ID
            AND la.award = 'Cy Young Award'
            AND la.winner = TRUE
      )
)
SELECT
    s.year_ID AS season,
    s.lg_ID AS league,
    s.name_common AS snubbed_pitcher,
    ROUND(s.pitcher_war, 1) AS snubbed_war,
    wn.name_common AS cy_young_winner,
    ROUND(s.winner_war, 1) AS winner_war,
    ROUND(s.war_diff, 1) AS war_advantage
FROM snubs s
LEFT JOIN winner_names wn ON s.winner_player_ID = wn.player_ID
ORDER BY s.war_diff DESC
LIMIT 20
