package com.michaelsgroi.baseballreference

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.sql.DriverManager
import java.time.Duration
import java.util.zip.ZipInputStream

object RetroDataLoader {
    private const val RETROSHEET_BASE = "https://www.retrosheet.org/gamelogs/"
    private const val CHADWICK_BASE = "https://raw.githubusercontent.com/chadwickbureau/register/master/data/"
    private const val CACHE_DIR = "data/retrosheet"
    private const val PARQUET_OUT = "data/derived"
    private val EXPIRATION = Duration.ofDays(30)

    // Gamelog fields (0-indexed): starting players begin at field 105, 3 fields each (id, name, pos) x 9 batters x 2 teams
    private const val VIS_STARTERS_START = 105
    private const val HOME_STARTERS_START = 132  // 105 + 27
    private const val FIELDS_PER_STARTER = 3

    fun download() {
        File(CACHE_DIR).mkdirs()
        val client = OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(2))
            .build()

        val years = (1871..2025).toList()
        val positions = mutableListOf<PositionRow>()

        println("downloading ${years.size} gamelog files from Retrosheet...")
        for (year in years) {
            val rows = loadGamelogYear(client, year)
            positions.addAll(rows)
            if (year % 10 == 0) println("  processed through $year (${positions.size} position records so far)")
        }
        println("total position records: ${positions.size}")

        println("downloading Chadwick register lookup...")
        val lookup = loadChadwickRegister(client)
        println("lookup entries: ${lookup.size}")

        // Write intermediate CSVs for parquet generation later
        writePositionsCsv(positions)
        writeChadwickCsv(lookup)
    }

    fun writeParquets() {
        File(PARQUET_OUT).mkdirs()
        writePositionsParquet()
        writeChadwickParquet()
    }

    private fun loadGamelogYear(client: OkHttpClient, year: Int): List<PositionRow> {
        val cacheFile = "$CACHE_DIR/gl$year.txt"
        val lines = if (File(cacheFile).exists() && !cacheFile.fileExpired(EXPIRATION)) {
            File(cacheFile).readLines()
        } else {
            val zipUrl = "$RETROSHEET_BASE/gl$year.zip"
            val bytes = try {
                client.newCall(Request.Builder().url(zipUrl).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return emptyList()
                    resp.body.bytes()
                }
            } catch (e: IOException) {
                return emptyList()
            }
            val text = ZipInputStream(bytes.inputStream()).use { zip ->
                generateSequence { zip.nextEntry }.firstOrNull() ?: return emptyList()
                zip.bufferedReader().readText()
            }
            File(cacheFile).writeText(text)
            text.lines()
        }

        val rows = mutableListOf<PositionRow>()
        for (line in lines) {
            if (line.isBlank()) continue
            val fields = parseGamelogLine(line)
            if (fields.size < 160) continue
            val yearId = fields[0].take(4).toIntOrNull() ?: continue

            // visiting starters (fields 105-131) and home starters (fields 132-158)
            for (teamOffset in listOf(VIS_STARTERS_START, HOME_STARTERS_START)) {
                for (batterSlot in 0 until 9) {
                    val base = teamOffset + batterSlot * FIELDS_PER_STARTER
                    if (base + 2 >= fields.size) continue
                    val retroId = fields[base].trim('"').trim()
                    val pos = fields[base + 2].trim('"').trim().toIntOrNull() ?: continue
                    if (retroId.isBlank() || retroId == "0") continue
                    rows.add(PositionRow(retroId, yearId, pos))
                }
            }
        }
        return rows
    }

    // Retrosheet gamelog lines are comma-separated but some fields are quoted
    private fun parseGamelogLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var i = 0
        val sb = StringBuilder()
        var inQuote = false
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> inQuote = !inQuote
                c == ',' && !inQuote -> { fields.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }

    private fun loadChadwickRegister(client: OkHttpClient): List<LookupRow> {
        val suffixes = ('0'..'9').map { it.toString() } + ('a'..'f').map { it.toString() }
        val rows = mutableListOf<LookupRow>()
        for (suffix in suffixes) {
            val cacheFile = "$CACHE_DIR/chadwick_people_$suffix.csv"
            val text = if (File(cacheFile).exists() && !cacheFile.fileExpired(EXPIRATION)) {
                File(cacheFile).readText()
            } else {
                val url = "$CHADWICK_BASE/people-$suffix.csv"
                val body = client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Failed to fetch $url: ${resp.code}")
                    resp.body.string()
                }
                File(cacheFile).writeText(body)
                body
            }
            val lines = text.lines()
            if (lines.isEmpty()) continue
            val header = lines[0].split(",")
            val retroIdx = header.indexOf("key_retro")
            val bbrefIdx = header.indexOf("key_bbref")
            if (retroIdx < 0 || bbrefIdx < 0) continue
            for (line in lines.drop(1)) {
                if (line.isBlank()) continue
                val fields = line.split(",")
                if (fields.size <= maxOf(retroIdx, bbrefIdx)) continue
                val retro = fields[retroIdx].trim()
                val bbref = fields[bbrefIdx].trim()
                if (retro.isNotBlank() && bbref.isNotBlank()) {
                    rows.add(LookupRow(retro, bbref))
                }
            }
        }
        return rows
    }

    private fun writePositionsCsv(positions: List<PositionRow>) {
        val csv = "$CACHE_DIR/positions.csv"
        File(csv).bufferedWriter().use { w ->
            w.write("retro_id,year_id,position\n")
            for (row in positions) {
                w.write("${row.retroId},${row.yearId},${row.position}\n")
            }
        }
    }

    private fun writeChadwickCsv(lookup: List<LookupRow>) {
        val csv = "$CACHE_DIR/lookup.csv"
        File(csv).bufferedWriter().use { w ->
            w.write("retro_id,bbref_id\n")
            for (row in lookup) {
                w.write("${row.retroId},${row.bbrefId}\n")
            }
        }
    }

    private fun writePositionsParquet() {
        val csv = "$CACHE_DIR/positions.csv"
        val parquet = "$PARQUET_OUT/retrosheet_positions.parquet"
        println("writing $parquet ...")
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    COPY (
                        WITH mapped AS (
                            SELECT retro_id, year_id,
                                CASE position
                                    WHEN 1  THEN 'P'
                                    WHEN 2  THEN 'C'
                                    WHEN 3  THEN '1B'
                                    WHEN 4  THEN '2B'
                                    WHEN 5  THEN '3B'
                                    WHEN 6  THEN 'SS'
                                    WHEN 7  THEN 'LF'
                                    WHEN 8  THEN 'CF'
                                    WHEN 9  THEN 'RF'
                                    WHEN 10 THEN 'DH'
                                    ELSE NULL
                                END AS pos
                            FROM read_csv_auto('$csv', header=true)
                        ),
                        counts AS (
                            SELECT retro_id, year_id, pos, COUNT(*) AS games
                            FROM mapped
                            WHERE pos IS NOT NULL
                            GROUP BY retro_id, year_id, pos
                        ),
                        totals AS (
                            SELECT retro_id, year_id, SUM(games) AS total_games
                            FROM counts
                            GROUP BY retro_id, year_id
                        )
                        SELECT c.retro_id, c.year_id,
                            STRING_AGG(c.pos, ',' ORDER BY c.games DESC) AS positions
                        FROM counts c
                        JOIN totals t ON t.retro_id = c.retro_id AND t.year_id = c.year_id
                        WHERE c.games >= 0.03 * t.total_games
                        GROUP BY c.retro_id, c.year_id
                    ) TO '$parquet' (FORMAT PARQUET)
                """.trimIndent())
            }
        }
        println("wrote $parquet")
    }

    private fun writeChadwickParquet() {
        val csv = "$CACHE_DIR/lookup.csv"
        val parquet = "$PARQUET_OUT/chadwick_lookup.parquet"
        println("writing $parquet ...")
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    COPY (SELECT * FROM read_csv_auto('$csv', header=true))
                    TO '$parquet' (FORMAT PARQUET)
                """.trimIndent())
            }
        }
        println("wrote $parquet")
    }

    private data class PositionRow(val retroId: String, val yearId: Int, val position: Int)
    private data class LookupRow(val retroId: String, val bbrefId: String)
}
