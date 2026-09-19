WITH all_war AS (
    SELECT player_id, name_common, year_id, SUM(war) AS season_war
    FROM (
        SELECT player_id, name_common, year_id, war FROM read_parquet('{{BAT}}') WHERE war IS NOT NULL
        UNION ALL
        SELECT player_id, name_common, year_id, war FROM read_parquet('{{PITCH}}') WHERE war IS NOT NULL
    ) combined
    GROUP BY player_id, name_common, year_id
),
negative_seasons AS (
    SELECT
        player_id,
        name_common,
        year_id,
        season_war,
        year_id - ROW_NUMBER() OVER (PARTITION BY player_id ORDER BY year_id) AS grp
    FROM all_war
    WHERE season_war < 0
),
streaks AS (
    SELECT
        player_id,
        MAX(name_common) AS name_common,
        grp,
        COUNT(*) AS consecutive_negative_seasons,
        MIN(year_id) AS streak_start,
        MAX(year_id) AS streak_end
    FROM negative_seasons
    GROUP BY player_id, grp
)
SELECT
    '[' || name_common || '](https://www.baseball-reference.com/players/' || LOWER(LEFT(player_id, 1)) || '/' || player_id || '.shtml)' AS player,
    consecutive_negative_seasons,
    streak_start,
    streak_end
FROM streaks
ORDER BY consecutive_negative_seasons DESC, streak_start
LIMIT 500
