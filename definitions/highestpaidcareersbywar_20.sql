WITH all_lines AS (
 SELECT player_id,name_common,year_id,UPPER(team_id) team_id,war,COALESCE(salary,0) salary,ROW_NUMBER() OVER() source_order FROM read_parquet('{{BAT}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
 UNION ALL
 SELECT player_id,name_common,year_id,UPPER(team_id),war,COALESCE(salary,0),1000000000+ROW_NUMBER() OVER() FROM read_parquet('{{PITCH}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
), seasons AS (
 SELECT player_id,year_id,SUM(war) season_war,CAST(ROUND(AVG(salary)) AS BIGINT) season_salary,MIN(source_order) season_order FROM all_lines GROUP BY player_id,year_id
), player_info AS (
 SELECT player_id,FIRST(name_common ORDER BY source_order) AS name,MIN(source_order) player_order FROM all_lines GROUP BY player_id
), careers AS (
 SELECT s.player_id,p.name,SUM(s.season_war) career_war,SUM(s.season_salary) salary,
 COUNT(*) season_count,MIN(s.year_id) min_year,MAX(s.year_id) max_year,p.player_order
 FROM seasons s JOIN player_info p USING(player_id) GROUP BY s.player_id,p.name,p.player_order
), teams AS (
 SELECT player_id,STRING_AGG(team_id,',' ORDER BY first_appearance) teams FROM (
  SELECT player_id,team_id,MIN(source_order) first_appearance FROM all_lines GROUP BY player_id,team_id
 ) GROUP BY player_id
), floor AS (SELECT MIN(career_war) lowest_war FROM careers)
SELECT c.name,ROUND(c.career_war,2) career_war,c.salary,c.season_count,'('||c.min_year||'-'||c.max_year||')' year_range,t.teams
FROM careers c JOIN teams t USING(player_id) CROSS JOIN floor WHERE c.salary>0
ORDER BY c.salary/(c.career_war-floor.lowest_war) DESC,c.player_order LIMIT 20
