SELECT
    name_common AS player,
    player_ID,
    ROUND(SUM(WAR_def), 1) AS career_dwar
FROM read_parquet('{{BAT}}')
WHERE WAR_def IS NOT NULL
GROUP BY name_common, player_ID
ORDER BY career_dwar DESC
LIMIT 10
