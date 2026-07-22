package com.michaelsgroi.baseballreference

import java.io.File
import java.sql.DriverManager

object WarParquet {
    private val PARQLO_DIR = (System.getenv("PARQLO_LOCAL") ?: "${System.getProperty("user.home")}/Documents/d/github/parqlo/data") + "/war"
    private val POSITIONS_PARQUET = "$PARQLO_DIR/retrosheet_positions.parquet"
    private val LOOKUP_PARQUET = "$PARQLO_DIR/chadwick_lookup.parquet"
    private const val LAHMAN_FIELDING_CSV = "data/lahman/Fielding.csv"
    private val LAHMAN_POSITIONS_PARQUET = "$PARQLO_DIR/lahman_positions.parquet"
    private const val LAHMAN_BATTING_CSV = "data/lahman/Batting.csv"
    private const val LAHMAN_PITCHING_CSV = "data/lahman/Pitching.csv"
    private const val LAHMAN_AWARDS_CSV = "data/lahman/AwardsPlayers.csv"
    private const val LAHMAN_ALLSTAR_CSV = "data/lahman/AllstarFull.csv"
    private const val LAHMAN_HOF_CSV = "data/lahman/HallOfFame.csv"
    private val LAHMAN_AWARDS_PARQUET = "$PARQLO_DIR/lahman_awards.parquet"
    private val LAHMAN_HOF_PARQUET = "$PARQLO_DIR/lahman_hof.parquet"

    fun generate() {
        File(PARQLO_DIR).mkdirs()
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
        val hasLahmanBatting = File(LAHMAN_BATTING_CSV).exists()
        val hasLahmanPitching = File(LAHMAN_PITCHING_CSV).exists()
        if (!hasLahmanBatting) println("lahman Batting.csv not found at $LAHMAN_BATTING_CSV — generating without lahman batting stats")
        if (!hasLahmanPitching) println("lahman Pitching.csv not found at $LAHMAN_PITCHING_CSV — generating without lahman pitching stats")
        if (File(LAHMAN_AWARDS_CSV).exists() && File(LAHMAN_ALLSTAR_CSV).exists()) {
            writeLahmanAwardsParquet()
        } else {
            println("lahman AwardsPlayers.csv/AllstarFull.csv not found — generating without lahman awards")
        }
        if (File(LAHMAN_HOF_CSV).exists()) {
            writeLahmanHofParquet()
        } else {
            println("lahman HallOfFame.csv not found at $LAHMAN_HOF_CSV — generating without lahman hof")
        }
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                listOf(
                    BrWarDaily.WAR_DAILY_BAT_FILE,
                    BrWarDaily.WAR_DAILY_PITCH_FILE,
                ).forEach { csv ->
                    val parquet = "$PARQLO_DIR/${csv.removeSuffix(".txt")}.parquet"
                    println("converting $csv -> $parquet ...")
                    val isPitching = csv == BrWarDaily.WAR_DAILY_PITCH_FILE
                    val pitchCsv = BrWarDaily.WAR_DAILY_PITCH_FILE
                    // Aggregated Lahman counting stats joined in as extra columns (SUM stints, rates recomputed).
                    // Lahman playerID matches bbref player_ID directly (same as writeLahmanPositionsParquet).
                    val statsSelect: String
                    val statsJoin: String
                    if (isPitching && hasLahmanPitching) {
                        statsSelect = """,
                            lp.W, lp.L, lp.lh_GS, lp.CG, lp.SHO, lp.SV, lp.IP, lp.H, lp.ER, lp.HR,
                            lp.BB, lp.SO, lp.ERA, lp.WP, lp.HBP, lp.BK, lp.BFP"""
                        statsJoin = """
                            LEFT JOIN (
                                SELECT playerID, yearID,
                                    SUM(W) AS W, SUM(L) AS L, SUM(GS) AS lh_GS, SUM(CG) AS CG,
                                    SUM(SHO) AS SHO, SUM(SV) AS SV, SUM(IPouts) / 3.0 AS IP,
                                    SUM(H) AS H, SUM(ER) AS ER, SUM(HR) AS HR, SUM(BB) AS BB, SUM(SO) AS SO,
                                    CASE WHEN SUM(IPouts) > 0 THEN SUM(ER) * 9 / (SUM(IPouts) / 3.0) ELSE NULL END AS ERA,
                                    SUM(WP) AS WP, SUM(HBP) AS HBP, SUM(BK) AS BK, SUM(BFP) AS BFP
                                FROM read_csv_auto('$LAHMAN_PITCHING_CSV', header=true, nullstr='NULL')
                                GROUP BY playerID, yearID
                            ) lp ON LOWER(b.player_ID) = LOWER(lp.playerID) AND b.year_ID = lp.yearID AND b.stint_ID = 1"""
                    } else if (!isPitching && hasLahmanBatting) {
                        statsSelect = """,
                            lb.AB, lb.R, lb.H, lb."2B", lb."3B", lb.HR, lb.RBI, lb.SB, lb.CS,
                            lb.BB, lb.SO, lb.HBP, lb.SF, lb.GIDP,
                            CASE WHEN lb.AB > 0 THEN lb.H * 1.0 / lb.AB ELSE NULL END AS AVG,
                            CASE WHEN (lb.AB + lb.BB + lb.HBP + lb.SF) > 0
                                 THEN (lb.H + lb.BB + lb.HBP) * 1.0 / (lb.AB + lb.BB + lb.HBP + lb.SF)
                                 ELSE NULL END AS OBP,
                            CASE WHEN lb.AB > 0
                                 THEN (lb.H + lb."2B" + 2 * lb."3B" + 3 * lb.HR) * 1.0 / lb.AB
                                 ELSE NULL END AS SLG"""
                        statsJoin = """
                            LEFT JOIN (
                                SELECT playerID, yearID,
                                    SUM(AB) AS AB, SUM(R) AS R, SUM(H) AS H, SUM("2B") AS "2B",
                                    SUM("3B") AS "3B", SUM(HR) AS HR, SUM(RBI) AS RBI, SUM(SB) AS SB,
                                    SUM(CS) AS CS, SUM(BB) AS BB, SUM(SO) AS SO, SUM(HBP) AS HBP,
                                    SUM(SF) AS SF, SUM(GIDP) AS GIDP
                                FROM read_csv_auto('$LAHMAN_BATTING_CSV', header=true, nullstr='NULL')
                                GROUP BY playerID, yearID
                            ) lb ON LOWER(b.player_ID) = LOWER(lb.playerID) AND b.year_ID = lb.yearID AND b.stint_ID = 1"""
                    } else {
                        statsSelect = ""
                        statsJoin = ""
                    }
                    val query = if (hasPositions) {
                        if (isPitching) {
                            """
                            SELECT b.*, COALESCE(p.positions, CASE WHEN b.GS > (b.G - b.GS) THEN 'SP' ELSE 'RP' END) AS positions$statsSelect
                            FROM read_csv_auto('$csv', header=true, nullstr='NULL') b
                            LEFT JOIN (
                                SELECT c.bbref_id, pos.year_id, pos.positions
                                FROM read_parquet('$LOOKUP_PARQUET') c
                                JOIN read_parquet('$POSITIONS_PARQUET') pos ON pos.retro_id = c.retro_id
                            ) p ON LOWER(b.player_ID) = LOWER(p.bbref_id) AND b.year_ID = p.year_id$statsJoin
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
                                ) AS position$statsSelect
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
                            ) pit ON LOWER(b.player_ID) = LOWER(pit.player_ID) AND b.year_ID = pit.year_ID$statsJoin
                            """.trimIndent()
                        }
                    } else if (statsSelect.isEmpty()) {
                        "SELECT * FROM read_csv_auto('$csv', header=true, nullstr='NULL')"
                    } else {
                        "SELECT b.*$statsSelect FROM read_csv_auto('$csv', header=true, nullstr='NULL') b$statsJoin"
                    }
                    stmt.execute("COPY ($query) TO '$parquet' (FORMAT PARQUET)")
                }
            }
        }
    }

    internal fun writeLahmanAwardsParquet(
        awardsCsv: String = LAHMAN_AWARDS_CSV,
        allstarCsv: String = LAHMAN_ALLSTAR_CSV,
        parquet: String = LAHMAN_AWARDS_PARQUET,
    ) {
        println("writing $parquet from $awardsCsv + $allstarCsv ...")
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                // One row per player-year-award. Lahman playerID matches bbref player_ID directly
                // (same convention as writeLahmanPositionsParquet). All-Star selections come from a
                // separate file and are folded in as award='All-Star'.
                stmt.execute("""
                    COPY (
                        SELECT playerID AS player_id, yearID AS year_id, awardID AS award, lgID AS lg_id
                        FROM read_csv_auto('$awardsCsv', header=true, nullstr='NULL')
                        UNION ALL
                        SELECT playerID AS player_id, yearID AS year_id, 'All-Star' AS award, lgID AS lg_id
                        FROM read_csv_auto('$allstarCsv', header=true, nullstr='NULL')
                    ) TO '$parquet' (FORMAT PARQUET)
                """.trimIndent())
            }
        }
        println("wrote $parquet")
    }

    internal fun writeLahmanHofParquet(
        hofCsv: String = LAHMAN_HOF_CSV,
        parquet: String = LAHMAN_HOF_PARQUET,
    ) {
        println("writing $parquet from $hofCsv ...")
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                // One row per player-ballot-year. vote_pct = votes/ballots*100; NULL ballots yield NULL.
                stmt.execute("""
                    COPY (
                        SELECT playerID AS player_id, yearID AS year_id, inducted,
                            votes, ballots,
                            CASE WHEN ballots > 0 THEN votes * 100.0 / ballots ELSE NULL END AS vote_pct,
                            votedBy AS voted_by
                        FROM read_csv_auto('$hofCsv', header=true, nullstr='NULL')
                    ) TO '$parquet' (FORMAT PARQUET)
                """.trimIndent())
            }
        }
        println("wrote $parquet")
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
