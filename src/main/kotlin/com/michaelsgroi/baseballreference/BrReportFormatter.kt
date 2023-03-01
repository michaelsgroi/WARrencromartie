package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrReports.*

class BrReportFormatter<T>(private val fields: List<Field<T>>) {

    fun format(report: Report<T>): List<String> {
        val rows = report.run()
        return listOf(
            report.name, // report name
            "${rows.size} rows", // row count
            "", // blank row
        ) + fields.joinToString(" ") { field -> // header row
            field.header.pad(field.length, field.alignRight)
        } + rows.mapIndexed { index, row -> // data rows
            fields.joinToString(" ") { field ->
                field.valueSupplier.invoke(index, row).pad(field.length, field.alignRight)
            }
        }
    }

    private fun String.pad(length: Int, alignRight: Boolean): String = if (alignRight) {
        padStart(length)
    } else {
        padEnd(length)
    }

    data class Field<T>(
        val header: String,
        val length: Int,
        val alignRight: Boolean,
        val valueSupplier: (Int, T) -> String
    )

}