package com.michaelsgroi.baseballreference

import java.io.File

object Parqlo {
    private const val WAR_JSON = "src/main/resources/parqlo/war.json"
    private const val SAMPLES_FILE = "src/main/resources/parqlo/sample_questions.txt"
    private const val DEFINITIONS_DIR = "definitions"
    val DEFAULT_TARGET = (System.getenv("PARQLO_LOCAL") ?: "${System.getProperty("user.home")}/Documents/d/github/parqlo/data") + "/war.json"

    fun generate(target: String = DEFAULT_TARGET) {
        val warJson = File(WAR_JSON).readText()
        val stems = File(SAMPLES_FILE).readLines().map { it.trim() }.filter { it.isNotEmpty() }
        val missing = stems.filter { !File("$DEFINITIONS_DIR/$it.sql").exists() || !File("$DEFINITIONS_DIR/$it.json").exists() }
        check(missing.isEmpty()) { "Unknown report(s) in sample_questions.txt: ${missing.joinToString()}" }
        val samples = stems.joinToString(",\n    ") { stem -> buildSample(stem) }
        val samplesBlock = "\"samples\": [\n    $samples\n  ]"
        val samplesStart = warJson.indexOf("\"samples\":")
        val arrayStart = warJson.indexOf('[', samplesStart)
        var depth = 0
        var arrayEnd = arrayStart
        for (i in arrayStart until warJson.length) {
            when (warJson[i]) { '[' -> depth++; ']' -> { depth--; if (depth == 0) { arrayEnd = i; break } } }
        }
        val updated = warJson.substring(0, samplesStart) + samplesBlock + warJson.substring(arrayEnd + 1)
        val targetFile = File(target)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(updated)
        println("wrote ${File(SAMPLES_FILE).readLines().count { it.isNotBlank() }} samples to $target")
    }

    private fun buildSample(stem: String): String {
        val meta = File("$DEFINITIONS_DIR/$stem.json").readText()
        val name = meta.substringAfter("\"name\":").substringAfter("\"").substringBefore("\"")
        val sql = File("$DEFINITIONS_DIR/$stem.sql").readText()
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace("read_parquet('{{BAT}}')", "\${batting}")
            .replace("read_parquet('{{PITCH}}')", "\${pitching}")
            .replace("read_parquet('{{AWARDS}}')", "\${lahman_awards}")
            .replace("read_parquet('{{HOF}}')", "\${lahman_hof}")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return """{"title": "$name", "sample": true, "sql": "$sql"}"""
    }
}
