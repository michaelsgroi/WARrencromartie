WITH batting_war AS (
    SELECT player_ID, year_ID,
           MAX(name_common) AS name_common,
           SUM(WAR) AS war
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL AND year_ID >= 1922
    GROUP BY player_ID, year_ID
),
pitching_war AS (
    SELECT player_ID, year_ID,
           MAX(name_common) AS name_common,
           SUM(WAR) AS war
    FROM read_parquet('{{PITCH}}')
    WHERE WAR IS NOT NULL AND year_ID >= 1922
    GROUP BY player_ID, year_ID
),
player_season_war AS (
    SELECT
        COALESCE(b.player_ID, p.player_ID) AS player_ID,
        COALESCE(b.year_ID, p.year_ID) AS year_ID,
        COALESCE(b.name_common, p.name_common) AS name_common,
        COALESCE(b.war, 0.0) + COALESCE(p.war, 0.0) AS total_war
    FROM batting_war b
    FULL OUTER JOIN pitching_war p
      ON b.player_ID = p.player_ID AND b.year_ID = p.year_ID
),
mvp_winners AS (
    SELECT player_ID, year_ID, lg_ID
    FROM read_parquet('{{AWARDS}}')
    WHERE award = 'MVP' AND winner = TRUE
),
mvp_war AS (
    SELECT
        m.year_ID,
        m.lg_ID,
        m.player_ID AS mvp_player_ID,
        ps.name_common AS mvp_name,
        ps.total_war AS mvp_total_war
    FROM mvp_winners m
    JOIN player_season_war ps
      ON ps.player_ID = m.player_ID AND ps.year_ID = m.year_ID
),
player_league AS (
    SELECT player_ID, year_ID, lg_ID,
           ROW_NUMBER() OVER (PARTITION BY player_ID, year_ID ORDER BY SUM(WAR) DESC) AS rn
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL AND year_ID >= 1922 AND lg_ID IN ('AL', 'NL')
    GROUP BY player_ID, year_ID, lg_ID
),
snubs AS (
    SELECT
        ps.player_ID,
        ps.year_ID,
        pl.lg_ID,
        ps.name_common AS player_name,
        ROUND(ps.total_war, 1) AS player_war,
        mw.mvp_player_ID,
        mw.mvp_name,
        ROUND(mw.mvp_total_war, 1) AS mvp_war,
        ROUND(ps.total_war - mw.mvp_total_war, 1) AS war_advantage
    FROM player_season_war ps
    JOIN player_league pl
      ON pl.player_ID = ps.player_ID AND pl.year_ID = ps.year_ID AND pl.rn = 1
    JOIN mvp_war mw
      ON mw.year_ID = ps.year_ID AND mw.lg_ID = pl.lg_ID
    WHERE ps.player_ID != mw.mvp_player_ID
      AND ps.total_war > mw.mvp_total_war
)
SELECT
    year_ID AS season,
    lg_ID AS league,
    player_name,
    player_war,
    mvp_name,
    mvp_war,
    war_advantage
FROM snubs
ORDER BY war_advantage DESC
LIMIT 20
