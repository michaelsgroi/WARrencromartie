SELECT
    name_common AS player,
    year_ID,
    team_ID,
    lg_ID,
    position,
    WAR_def
FROM read_parquet('{{BAT}}')
WHERE WAR_def IS NOT NULL
ORDER BY WAR_def DESC
LIMIT 10
