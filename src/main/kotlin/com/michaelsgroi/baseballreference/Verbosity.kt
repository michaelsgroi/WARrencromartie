package com.michaelsgroi.baseballreference

enum class Verbosity(private val s: String) {
    CONCISE("concise"),
    VERBOSE("");

    override fun toString() = this.s
}
