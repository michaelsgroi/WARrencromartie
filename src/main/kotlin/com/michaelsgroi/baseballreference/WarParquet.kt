package com.michaelsgroi.baseballreference

import java.io.File
import java.sql.DriverManager

object WarParquet {
    private val PARQUET_OUT = (System.getenv("PARQLO_LOCAL") ?: "${System.getProperty("user.home")}/Documents/d/github/parqlo/data") + "/war"
    private val POSITIONS_PARQUET = "$PARQUET_OUT/retrosheet_positions.parquet"
    private val LOOKUP_PARQUET = "$PARQUET_OUT/chadwick_lookup.parquet"
    private const val LAHMAN_FIELDING_CSV = "data/lahman/Fielding.csv"
    private val LAHMAN_POSITIONS_PARQUET = "$PARQUET_OUT/lahman_positions.parquet"

    fun generate() {
        File(PARQUET_OUT).mkdirs()
        val hasPositions = File(POSITIONS_PARQUET).exists() && File(LOOKUP_PARQUET).exists()
        if (!hasPositions) {
            println("retrosheet position data not found — run 'make retrosheet' first; generating without")
        }
        val hasLahman = File(LAHMAN_FIELDING_CSV).exists()
        if (hasLahman) {
            writeLahmanPositionsParquet()
        } else {
            println("lahman Fielding.csv not found at $LAHMAN_FIELDING_CSV — generating without lahman positions")
        }
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                listOf(
                    BrWarDaily.WAR_DAILY_BAT_FILE,
                    BrWarDaily.WAR_DAILY_PITCH_FILE,
                ).forEach { csv ->
                    val parquet = "$PARQUET_OUT/${csv.removeSuffix(".txt")}.parquet"
                    println("converting $csv -> $parquet ...")
                    val isPitching = csv == BrWarDaily.WAR_DAILY_PITCH_FILE
                    val pitchCsv = BrWarDaily.WAR_DAILY_PITCH_FILE
                    val query = if (hasPositions) {
                        if (isPitching) {
                            """
                            SELECT b.*, COALESCE(p.positions, CASE WHEN b.GS > (b.G - b.GS) THEN 'SP' ELSE 'RP' END) AS positions
                            FROM read_csv_auto('$csv', header=true, nullstr='NULL') b
                            LEFT JOIN (
                                SELECT c.bbref_id, pos.year_id, pos.positions
                                FROM read_parquet('$LOOKUP_PARQUET') c
                                JOIN read_parquet('$POSITIONS_PARQUET') pos ON pos.retro_id = c.retro_id
                            ) p ON LOWER(b.player_ID) = LOWER(p.bbref_id) AND b.year_ID = p.year_id
                            """.trimIndent()
                        } else {
                            val pitcherInference = """
                                CASE WHEN pit.player_ID IS NOT NULL
                                          AND (b.year_ID < 1973
                                               OR (b.year_ID BETWEEN 1973 AND 2022 AND b.lg_ID = 'NL')
                                               OR b.year_ID > 2022)
                                     THEN CASE WHEN pit.GS > (pit.G - pit.GS) THEN 'SP' ELSE 'RP' END
                                     ELSE NULL END
                            """.trimIndent()
                            val lahmanPositionsCol = if (hasLahman) "lah.positions" else "NULL"
                            val lahmanCoalesce = if (hasLahman) "lah.positions," else ""
                            val lahmanJoin = if (hasLahman) """
                            LEFT JOIN read_parquet('$LAHMAN_POSITIONS_PARQUET') lah
                              ON LOWER(b.player_ID) = LOWER(lah.player_id) AND b.year_ID = lah.year_id
                            """ else ""
                            """
                            SELECT b.*,
                                p.positions AS positions_retro,
                                $lahmanPositionsCol AS positions_lahman,
                                COALESCE(
                                    p.positions,
                                    $lahmanCoalesce
                                    $pitcherInference
                                ) AS position
                            FROM read_csv_auto('$csv', header=true, nullstr='NULL') b
                            LEFT JOIN (
                                SELECT c.bbref_id, pos.year_id, pos.positions
                                FROM read_parquet('$LOOKUP_PARQUET') c
                                JOIN read_parquet('$POSITIONS_PARQUET') pos ON pos.retro_id = c.retro_id
                            ) p ON LOWER(b.player_ID) = LOWER(p.bbref_id) AND b.year_ID = p.year_id
                            $lahmanJoin
                            LEFT JOIN (
                                SELECT player_ID, year_ID, G, GS
                                FROM read_csv_auto('$pitchCsv', header=true, nullstr='NULL')
                            ) pit ON LOWER(b.player_ID) = LOWER(pit.player_ID) AND b.year_ID = pit.year_ID
                            """.trimIndent()
                        }
                    } else {
                        "SELECT * FROM read_csv_auto('$csv', header=true, nullstr='NULL')"
                    }
                    stmt.execute("COPY ($query) TO '$parquet' (FORMAT PARQUET)")
                }
            }
        }
    }

    private fun writeLahmanPositionsParquet() {
        val parquet = LAHMAN_POSITIONS_PARQUET
        println("writing $parquet from $LAHMAN_FIELDING_CSV ...")
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                // Aggregate Lahman fielding into comma-separated position list per player-season,
                // filtering PH/PR (no fielding position), applying 3% threshold, ordering by games desc.
                // Lahman POS codes mapped to display strings; OF stays as OF (no LF/CF/RF split in Lahman).
                stmt.execute("""
                    COPY (
                        WITH mapped AS (
                            SELECT playerID AS player_id, yearID AS year_id,
                                CASE POS
                                    WHEN 'P'  THEN 'P'
                                    WHEN 'C'  THEN 'C'
                                    WHEN '1B' THEN '1B'
                                    WHEN '2B' THEN '2B'
                                    WHEN '3B' THEN '3B'
                                    WHEN 'SS' THEN 'SS'
                                    WHEN 'OF' THEN 'OF'
                                    ELSE NULL
                                END AS pos,
                                G AS games
                            FROM read_csv_auto('$LAHMAN_FIELDING_CSV', header=true)
                            WHERE POS NOT IN ('PH', 'PR')
                        ),
                        filtered AS (
                            SELECT player_id, year_id, pos, games
                            FROM mapped
                            WHERE pos IS NOT NULL
                        ),
                        totals AS (
                            SELECT player_id, year_id, SUM(games) AS total_games
                            FROM filtered
                            GROUP BY player_id, year_id
                        )
                        SELECT f.player_id, f.year_id,
                            STRING_AGG(f.pos, ',' ORDER BY f.games DESC) AS positions
                        FROM filtered f
                        JOIN totals t ON t.player_id = f.player_id AND t.year_id = f.year_id
                        WHERE f.games >= 0.03 * t.total_games
                        GROUP BY f.player_id, f.year_id
                    ) TO '$parquet' (FORMAT PARQUET)
                """.trimIndent())
            }
        }
        println("wrote $parquet")
    }
}
