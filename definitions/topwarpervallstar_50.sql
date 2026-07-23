WITH player_wars AS (
    SELECT
        player_ID,
        MAX(name_common) AS name_common,
        SUM(WAR) AS career_war
    FROM (
        SELECT player_ID, name_common, WAR FROM read_parquet('{{BAT}}')
        WHERE WAR IS NOT NULL AND year_ID >= 1933
        UNION ALL
        SELECT player_ID, name_common, WAR FROM read_parquet('{{PITCH}}')
        WHERE WAR IS NOT NULL AND year_ID >= 1933
    )
    GROUP BY player_ID
),
allstar_counts AS (
    SELECT
        player_ID,
        COUNT(*) AS allstar_appearances
    FROM read_parquet('{{AWARDS}}')
    WHERE award = 'All-Star'
      AND winner = TRUE
      AND year_ID >= 1933
    GROUP BY player_ID
)
SELECT
    name_common AS player,
    ROUND(pw.career_war, 1) AS career_war,
    ac.allstar_appearances,
    ROUND(pw.career_war / ac.allstar_appearances, 2) AS war_per_allstar
FROM player_wars pw
INNER JOIN allstar_counts ac ON pw.player_ID = ac.player_ID
ORDER BY war_per_allstar DESC
LIMIT 50
