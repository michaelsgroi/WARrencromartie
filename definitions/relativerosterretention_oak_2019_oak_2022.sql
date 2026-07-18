WITH all_seasons AS (
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
    FROM read_parquet('{{BAT}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
    UNION ALL
    SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
    FROM read_parquet('{{PITCH}}')
    WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
),
player_seasons AS (
    SELECT player_id, FIRST(name_common) AS name, year_id,
        ROUND(SUM(war), 2) AS war,
        STRING_AGG(DISTINCT team_id, ',' ORDER BY team_id) AS teams
    FROM all_seasons
    GROUP BY player_id, year_id
),
source_ranked AS (
    SELECT ps.*, ROW_NUMBER() OVER (ORDER BY war DESC) AS war_rank
    FROM player_seasons ps
    WHERE year_id = 2019
      AND player_id IN (
          SELECT player_id FROM all_seasons
          WHERE year_id = 2019 AND team_id = 'OAK'
      )
),
retained_ranks AS (
    SELECT war_rank
    FROM source_ranked
    WHERE player_id IN (
        SELECT player_id FROM all_seasons
        WHERE year_id = 2022 AND team_id = 'OAK'
    )
)
SELECT name, war, year_id AS year, teams
FROM source_ranked
WHERE war_rank IN (SELECT war_rank FROM retained_ranks)
ORDER BY war DESC, name
