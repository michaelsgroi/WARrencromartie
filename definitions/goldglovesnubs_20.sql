WITH gold_glove_winners AS (
    SELECT player_ID, year_ID
    FROM read_parquet('{{AWARDS}}')
    WHERE award = 'Gold Glove'
      AND winner = TRUE
),
batting_dwar AS (
    SELECT
        b.player_ID,
        b.year_ID,
        MAX(b.name_common) AS name_common,
        MAX(UPPER(b.team_ID)) AS team,
        MAX(b.position) AS position,
        SUM(b.WAR_def) AS dwar
    FROM read_parquet('{{BAT}}') b
    WHERE b.WAR_def IS NOT NULL
      AND b.year_ID >= 1957
      AND b.lg_ID IN ('AL', 'NL')
    GROUP BY b.player_ID, b.year_ID
)
SELECT
    bd.name_common AS player,
    bd.year_ID AS season,
    bd.team,
    bd.position,
    ROUND(bd.dwar, 1) AS dWAR
FROM batting_dwar bd
LEFT JOIN gold_glove_winners gg
    ON bd.player_ID = gg.player_ID
   AND bd.year_ID = gg.year_ID
WHERE gg.player_ID IS NULL
ORDER BY bd.dwar DESC
LIMIT 20
