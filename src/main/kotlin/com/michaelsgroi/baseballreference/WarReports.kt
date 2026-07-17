package com.michaelsgroi.baseballreference

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object WarReports {
    private const val BAT = "data/derived/war_daily_bat.parquet"
    private const val PITCH = "data/derived/war_daily_pitch.parquet"
    private const val REPORT_DIR = "reports"

    fun run() {
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            bestCareerWar(conn, 50)
            topCareerWar(conn, 10)
        }
    }

    private fun bestCareerWar(conn: Connection, topN: Int) {
        val sql = """
            WITH seasons AS (
                SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
                FROM read_parquet('$BAT')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
                UNION ALL
                SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
                FROM read_parquet('$PITCH')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
            ),
            careers AS (
                SELECT
                    player_id,
                    FIRST(name_common ORDER BY year_id) AS name,
                    ROUND(SUM(war), 2) AS career_war,
                    COUNT(DISTINCT year_id) AS season_count,
                    MIN(year_id) AS min_year,
                    MAX(year_id) AS max_year
                FROM seasons
                GROUP BY player_id
            ),
            rows_numbered AS (
                SELECT player_id, UPPER(team_id) AS team_id,
                    ROW_NUMBER() OVER () AS rn
                FROM read_parquet('$BAT')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
                UNION ALL
                SELECT player_id, UPPER(team_id) AS team_id,
                    ROW_NUMBER() OVER () AS rn
                FROM read_parquet('$PITCH')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
            ),
            player_teams AS (
                SELECT player_id, team_id, MIN(rn) AS first_appearance
                FROM rows_numbered
                GROUP BY player_id, team_id
            ),
            teams AS (
                SELECT
                    player_id,
                    STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
                FROM player_teams
                GROUP BY player_id
            )
            SELECT c.name, c.career_war, c.season_count, c.min_year, c.max_year, t.teams
            FROM careers c
            JOIN teams t USING (player_id)
            ORDER BY c.career_war DESC
        """.trimIndent()

        val rows = mutableListOf<List<String>>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                var rank = 0
                while (rs.next()) {
                    rank++
                    val name = rs.getString("name")
                    val war = rs.getString("career_war")
                    val seasons = rs.getInt("season_count")
                    val minYear = rs.getInt("min_year")
                    val maxYear = rs.getInt("max_year")
                    val teams = rs.getString("teams")
                    rows.add(listOf(
                        "#$rank:",
                        name,
                        war,
                        seasons.toString(),
                        "($minYear-$maxYear)",
                        teams,
                    ))
                }
            }
        }

        val cols = listOf(
            Col("#", 5, Align.RIGHT),
            Col("name", 20, Align.LEFT),
            Col("war", 10, Align.RIGHT),
            Col("seasons", 7, Align.RIGHT),
            Col("", 11, Align.LEFT),
            Col("teams", 256, Align.LEFT),
        )
        writeReport("bestcareerwar_${topN}.txt", "Best career war $topN", cols, rows)
    }

    private fun topCareerWar(conn: Connection, topN: Int) {
        val sql = """
            WITH all_seasons AS (
                SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
                FROM read_parquet('$BAT')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
                UNION ALL
                SELECT player_id, name_common, year_id, UPPER(team_id) AS team_id, war
                FROM read_parquet('$PITCH')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
            ),
            careers AS (
                SELECT player_id,
                    FIRST(name_common ORDER BY year_id) AS name,
                    ROUND(SUM(war), 2) AS career_war,
                    COUNT(DISTINCT year_id) AS season_count,
                    MIN(year_id) AS min_year,
                    MAX(year_id) AS max_year
                FROM all_seasons GROUP BY player_id
            ),
            rows_numbered AS (
                SELECT player_id, UPPER(team_id) AS team_id, ROW_NUMBER() OVER () AS rn
                FROM read_parquet('$BAT')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
                UNION ALL
                SELECT player_id, UPPER(team_id) AS team_id, ROW_NUMBER() OVER () AS rn
                FROM read_parquet('$PITCH')
                WHERE war IS NOT NULL AND lg_id IN ('AL', 'NL')
            ),
            player_teams AS (
                SELECT player_id, team_id, MIN(rn) AS first_appearance
                FROM rows_numbered GROUP BY player_id, team_id
            ),
            teams AS (
                SELECT player_id, STRING_AGG(team_id, ',' ORDER BY first_appearance) AS teams
                FROM player_teams GROUP BY player_id
            )
            SELECT c.name, c.career_war, c.season_count, c.min_year, c.max_year, t.teams
            FROM careers c JOIN teams t USING (player_id)
            ORDER BY c.career_war DESC
            LIMIT $topN
        """.trimIndent()

        val rows = mutableListOf<List<String>>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                var rank = 0
                while (rs.next()) {
                    rank++
                    rows.add(listOf(
                        "#$rank:",
                        rs.getString("name"),
                        rs.getString("career_war"),
                        rs.getInt("season_count").toString(),
                        "(${rs.getInt("min_year")}-${rs.getInt("max_year")})",
                        rs.getString("teams"),
                    ))
                }
            }
        }
        val cols = listOf(
            Col("#", 5, Align.RIGHT),
            Col("name", 20, Align.LEFT),
            Col("war", 10, Align.RIGHT),
            Col("seasons", 7, Align.RIGHT),
            Col("", 11, Align.LEFT),
            Col("teams", 256, Align.LEFT),
        )
        writeReport("topcareerwar_${topN}.txt", "Top career war $topN", cols, rows)
    }

    private enum class Align { LEFT, RIGHT }

    private data class Col(val header: String, val width: Int, val align: Align)

    private fun Col.fmt(v: String) = if (align == Align.RIGHT) v.padStart(width) else v.padEnd(width)

    private fun writeReport(filename: String, name: String, cols: List<Col>, rows: List<List<String>>) {
        val header = cols.joinToString(" ") { it.fmt(it.header) }
        val dataRows = rows.map { row -> cols.zip(row).joinToString(" ") { (c, v) -> c.fmt(v) } }
        val lines = listOf(name, "${rows.size} rows", "", header) + dataRows
        File("$REPORT_DIR/$filename").writeText(lines.joinToString("\n") { it.trimEnd() })
    }
}
