WITH player_seasons AS (
    SELECT
        player_ID,
        name_common,
        year_ID,
        SUM(WAR) AS season_war,
        SPLIT_PART(position, ',', 1) AS primary_pos
    FROM read_parquet('{{BAT}}')
    WHERE WAR IS NOT NULL
      AND position IS NOT NULL
    GROUP BY player_ID, name_common, year_ID, SPLIT_PART(position, ',', 1)
),
player_primary_position AS (
    SELECT
        player_ID,
        name_common,
        primary_pos,
        COUNT(*) AS seasons_at_pos
    FROM player_seasons
    GROUP BY player_ID, name_common, primary_pos
),
player_top_position AS (
    SELECT DISTINCT ON (player_ID)
        player_ID,
        name_common,
        primary_pos,
        seasons_at_pos
    FROM player_primary_position
    ORDER BY player_ID, seasons_at_pos DESC
),
player_career_war AS (
    SELECT
        b.player_ID,
        b.name_common,
        p.primary_pos,
        p.seasons_at_pos,
        SUM(b.WAR) AS career_war,
        COUNT(DISTINCT b.year_ID) AS total_seasons
    FROM read_parquet('{{BAT}}') b
    JOIN player_top_position p ON b.player_ID = p.player_ID
    WHERE b.WAR IS NOT NULL
    GROUP BY b.player_ID, b.name_common, p.primary_pos, p.seasons_at_pos
    HAVING p.seasons_at_pos >= 10
),
ranked AS (
    SELECT *,
        RANK() OVER (PARTITION BY primary_pos ORDER BY career_war ASC) AS rnk
    FROM player_career_war
)
SELECT
    primary_pos AS position,
    name_common AS player,
    seasons_at_pos AS seasons_at_primary_position,
    ROUND(career_war, 1) AS career_war
FROM ranked
WHERE rnk = 1
ORDER BY
    CASE primary_pos
        WHEN 'C'  THEN 1
        WHEN '1B' THEN 2
        WHEN '2B' THEN 3
        WHEN '3B' THEN 4
        WHEN 'SS' THEN 5
        WHEN 'LF' THEN 6
        WHEN 'CF' THEN 7
        WHEN 'RF' THEN 8
        WHEN 'OF' THEN 9
        WHEN 'DH' THEN 10
        WHEN 'P'  THEN 11
        ELSE 12
    END
