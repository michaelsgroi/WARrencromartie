package com.michaelsgroi.baseballreference

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager

class WarParquetTest {

    @Test
    fun awardsParquetFoldsAllStarSelections(@TempDir tmp: Path) {
        val awardsCsv = File(tmp.toFile(), "AwardsPlayers.csv").apply {
            writeText(
                """
                playerID,awardID,yearID,lgID,tie,notes
                ruthba01,MVP,1923,AL,,
                mayswi01,Gold Glove,1957,NL,,
                """.trimIndent()
            )
        }.path
        val allstarCsv = File(tmp.toFile(), "AllstarFull.csv").apply {
            writeText(
                """
                playerID,yearID,gameNum,gameID,teamID,lgID,GP,startingPos
                ruthba01,1933,0,ALS193307060,NYA,AL,1,9
                """.trimIndent()
            )
        }.path
        val parquet = File(tmp.toFile(), "lahman_awards.parquet").path

        WarParquet.writeLahmanAwardsParquet(awardsCsv, allstarCsv, parquet)

        val rows = readRows(parquet, "player_ID, year_ID, award, lg_ID", "year_ID, award")
        assertEquals(
            listOf(
                listOf("ruthba01", 1923, "MVP", "AL"),
                listOf("ruthba01", 1933, "All-Star", "AL"),
                listOf("mayswi01", 1957, "Gold Glove", "NL"),
            ),
            rows,
        )
    }

    @Test
    fun hofParquetComputesVotePercentage(@TempDir tmp: Path) {
        val hofCsv = File(tmp.toFile(), "HallOfFame.csv").apply {
            writeText(
                """
                playerID,yearID,votedBy,ballots,needed,votes,inducted,category,needed_note
                ruthba01,1936,BBWAA,226,170,215,Y,Player,
                mayswi01,1979,BBWAA,432,324,409,Y,Player,
                """.trimIndent()
            )
        }.path
        val parquet = File(tmp.toFile(), "lahman_hof.parquet").path

        WarParquet.writeLahmanHofParquet(hofCsv, parquet)

        val rows = readRows(
            parquet,
            "player_ID, year_ID, inducted, votes, ballots, ROUND(vote_pct, 4), voted_by",
            "year_ID",
        )
        assertEquals(
            listOf(
                listOf("ruthba01", 1936, "Y", 215, 226, 95.1327, "BBWAA"),
                listOf("mayswi01", 1979, "Y", 409, 432, 94.6759, "BBWAA"),
            ),
            rows,
        )
    }

    private fun readRows(parquet: String, selectList: String, orderBy: String): List<List<Any?>> {
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT $selectList FROM read_parquet('$parquet') ORDER BY $orderBy")
                val cols = rs.metaData.columnCount
                val out = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    out.add((1..cols).map { i ->
                        val v = rs.getObject(i)
                        when (v) {
                            is Number -> if (v.toDouble() == v.toLong().toDouble() && v !is Double) v.toInt() else v.toDouble()
                            else -> v
                        }
                    })
                }
                return out
            }
        }
    }
}
