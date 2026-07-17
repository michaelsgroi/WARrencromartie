package com.michaelsgroi.baseballreference

import java.io.File
import java.sql.DriverManager

object WarParquet {
    private const val PARQUET_OUT = "data/derived"

    fun generate() {
        File(PARQUET_OUT).mkdirs()
        Class.forName("org.duckdb.DuckDBDriver")
        DriverManager.getConnection("jdbc:duckdb:").use { conn ->
            conn.createStatement().use { stmt ->
                listOf(
                    BrWarDaily.WAR_DAILY_BAT_FILE,
                    BrWarDaily.WAR_DAILY_PITCH_FILE,
                ).forEach { csv ->
                    val parquet = "$PARQUET_OUT/${csv.removeSuffix(".txt")}.parquet"
                    println("converting $csv -> $parquet ...")
                    stmt.execute(
                        "COPY (SELECT * FROM read_csv_auto('$csv', header=true, nullstr='NULL')) " +
                            "TO '$parquet' (FORMAT PARQUET)",
                    )
                }
            }
        }
    }
}
