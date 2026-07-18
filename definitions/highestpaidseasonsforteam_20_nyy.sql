WITH all_lines AS (
 SELECT player_id,name_common,year_id,UPPER(team_id) team_id,war,COALESCE(salary,0) salary,ROW_NUMBER() OVER() source_order FROM read_parquet('{{BAT}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
 UNION ALL
 SELECT player_id,name_common,year_id,UPPER(team_id),war,COALESCE(salary,0),1000000000+ROW_NUMBER() OVER() FROM read_parquet('{{PITCH}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
), seasons AS (
 SELECT player_id,FIRST(name_common ORDER BY source_order) AS name,year_id,SUM(war) season_war,CAST(ROUND(AVG(salary)) AS BIGINT) salary,MIN(source_order) season_order FROM all_lines GROUP BY player_id,year_id
), teams AS (
 SELECT player_id,year_id,STRING_AGG(team_id,',' ORDER BY first_appearance) teams,BOOL_OR(team_id='NYY') is_team FROM (SELECT player_id,year_id,team_id,MIN(source_order) first_appearance FROM all_lines GROUP BY player_id,year_id,team_id) GROUP BY player_id,year_id
)
SELECT s.name,ROUND(s.season_war,2) war,s.salary,s.year_id AS "year",t.teams FROM seasons s JOIN teams t USING(player_id,year_id) WHERE t.is_team
ORDER BY s.salary DESC,s.season_order LIMIT 20
