package com.michaelsgroi.baseballreference

class BrReportFormatter<T>(
    private val fields: List<Field<T>>,
) {
    data class Field<T>(
        private val header: String, // TODO allow for header that overflows into next field
        private val fieldPadder: Padder,
        private val valueSupplier: (Int, T) -> String,
    ) {
        fun getHeader(): String = fieldPadder.pad(header)

        fun getField(
            index: Int,
            row: T,
        ): String = fieldPadder.pad(valueSupplier(index, row))
    }

    companion object {
        fun leftAlign(length: Int) = Padder(length) { it.padEnd(length) }

        fun rightAlign(length: Int) = Padder(length) { it.padStart(length) }

        fun asIs(length: Int) = Padder(length) { it }
    }

    class Padder(
        val length: Int,
        private val padder: (String) -> String,
    ) {
        fun pad(string: String): String = padder(string)
    }
}
