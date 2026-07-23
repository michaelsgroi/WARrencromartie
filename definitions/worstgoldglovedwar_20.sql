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
        SUM(b.WAR_def) AS dWAR
    FROM read_parquet('{{BAT}}') b
    WHERE b.WAR_def IS NOT NULL
    GROUP BY b.player_ID, b.year_ID
)
SELECT
    bw.name_common AS player,
    bw.year_ID AS season,
    bw.team,
    bw.position,
    ROUND(bw.dWAR, 1) AS dWAR
FROM batting_dwar bw
INNER JOIN gold_glove_winners gg
    ON bw.player_ID = gg.player_ID
   AND bw.year_ID = gg.year_ID
ORDER BY bw.dWAR ASC
LIMIT 20
