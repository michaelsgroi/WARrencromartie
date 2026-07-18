WITH all_lines AS (
 SELECT player_id,name_common,year_id,UPPER(team_id) team_id,war,ROW_NUMBER() OVER() source_order FROM read_parquet('{{BAT}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
 UNION ALL
 SELECT player_id,name_common,year_id,UPPER(team_id),war,1000000000+ROW_NUMBER() OVER() FROM read_parquet('{{PITCH}}') WHERE war IS NOT NULL AND lg_id IN('AL','NL')
), seasons AS (
 SELECT player_id,year_id,SUM(war) season_war,MIN(source_order) season_order FROM all_lines GROUP BY player_id,year_id
), player_info AS (
 SELECT player_id,FIRST(name_common ORDER BY source_order) AS name,MIN(source_order) player_order FROM all_lines GROUP BY player_id
), careers AS (
 SELECT s.player_id,p.name,p.player_order,SUM(s.season_war) career_war,COUNT(*) season_count,MIN(s.year_id) min_year,MAX(s.year_id) max_year
 FROM seasons s JOIN player_info p USING(player_id) GROUP BY s.player_id,p.name,p.player_order HAVING SUM(s.season_war)<10
), ranked AS (
 SELECT s.*,ROW_NUMBER() OVER(ORDER BY s.season_war DESC,s.season_order) overall_rank FROM seasons s JOIN careers c USING(player_id)
), selected AS (SELECT * FROM ranked WHERE overall_rank<=20),
teams AS (
 SELECT player_id,STRING_AGG(team_id,',' ORDER BY first_appearance) teams FROM (SELECT player_id,team_id,MIN(source_order) first_appearance FROM all_lines GROUP BY player_id,team_id) GROUP BY player_id
)
SELECT c.name,ROUND(c.career_war,2) career_war,CAST(ROUND(s.season_war,2) AS VARCHAR)||' ('||s.year_id||')' peak_war,
 c.season_count,'('||c.min_year||'-'||c.max_year||')' year_range,t.teams
FROM selected s JOIN careers c USING(player_id) JOIN teams t USING(player_id) ORDER BY s.overall_rank
