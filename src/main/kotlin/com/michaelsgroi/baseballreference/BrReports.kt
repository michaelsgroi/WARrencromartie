package com.michaelsgroi.baseballreference

import java.io.File
import java.sql.DriverManager

object BrReports {
    private val PARQLO_DIR = (System.getenv("PARQLO_LOCAL") ?: "${System.getProperty("user.home")}/Documents/d/github/parqlo/data") + "/war"
    private val BAT = "$PARQLO_DIR/war_daily_bat.parquet"
    private val PITCH = "$PARQLO_DIR/war_daily_pitch.parquet"
    private const val REPORT_DIR = "reports"
    private const val SQL_DIR = "definitions"

    fun run(filter: String? = null) {
        File(REPORT_DIR).mkdirs()
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            File(SQL_DIR).listFiles { f -> f.extension == "json" }
                ?.sortedBy { it.name }
                ?.filter { filter == null || it.nameWithoutExtension == filter }
                ?.forEach { jsonFile ->
                    val sqlFile = File(SQL_DIR, jsonFile.nameWithoutExtension + ".sql")
                    if (!sqlFile.exists()) {
                        System.err.println("No SQL file for ${jsonFile.name}, skipping")
                        return@forEach
                    }
                    val spec = parseSpec(jsonFile.readText())
                    val sql = sqlFile.readText()
                        .replace("{{BAT}}", BAT)
                        .replace("{{PITCH}}", PITCH)
                    val rows = mutableListOf<List<String>>()
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery(sql).use { rs ->
                            var rank = 0
                            while (rs.next()) {
                                rank++
                                rows.add(spec.columns.map { col ->
                                    when {
                                        col.autoRank -> "#$rank:"
                                        else -> rs.getString(col.field) ?: ""
                                    }
                                })
                            }
                        }
                    }
                    val cols = spec.columns.map { Col(it.header, it.width, if (it.align == "right") Align.RIGHT else Align.LEFT) }
                    writeReport(spec.filename, spec.name, cols, rows)
                }
        }
    }

    private data class ColSpec(val header: String, val width: Int, val align: String, val field: String, val autoRank: Boolean)
    private data class ReportSpec(val name: String, val filename: String, val columns: List<ColSpec>)

    private fun parseSpec(json: String): ReportSpec {
        val name = json.substringAfter("\"name\":").substringAfter("\"").substringBefore("\"")
        val filename = json.substringAfter("\"filename\":").substringAfter("\"").substringBefore("\"")
        val colsJson = json.substringAfter("\"columns\":").substringAfter("[").substringBeforeLast("]")
        val columns = colsJson.split("\\}\\s*,\\s*\\{".toRegex())
            .map { it.replace("{", "").replace("}", "") }
            .map { block ->
                fun field(key: String) = block.substringAfter("\"$key\":").trim().let {
                    if (it.startsWith("\"")) it.substringAfter("\"").substringBefore("\"")
                    else it.substringBefore(",").substringBefore("\n").trim()
                }
                ColSpec(
                    header = field("header"),
                    width = field("width").toIntOrNull() ?: 0,
                    align = field("align"),
                    field = runCatching { field("field") }.getOrDefault(""),
                    autoRank = block.contains("\"autoRank\": true") || block.contains("\"autoRank\":true"),
                )
            }
        return ReportSpec(name, filename, columns)
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
