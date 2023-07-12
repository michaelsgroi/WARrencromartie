package com.michaelsgroi.allstar2023

import com.michaelsgroi.baseballreference.readFile
import java.lang.NumberFormatException
import java.lang.RuntimeException
import kotlin.math.roundToInt

class FanGraphsWar(private val filename: String) {

    fun getPlayerWars() = deserializeToPlayerWar(filename.readFile().drop(1))

    private fun deserializeToPlayerWar(fanGraphsWarLines: List<String>) =
        fanGraphsWarLines.map { it.toPlayerWar() }

    data class PlayerWar(
        val name: String,
        val team: String,
        val bwar: Double,
        val pwar: Double,
    ) {
        fun twar() = bwar + pwar

        override fun toString(): String =
            "PlayerWar(name='$name', bwar=$bwar, pwar=$pwar, twar=${twar()})"
    }

    private fun String.stripQuotes(): String {
        val leadingStripped = if (this.startsWith("\"")) {
            this.substring(1, this.length - 1)
        } else {
            this
        }
        return if (leadingStripped.endsWith("\"")) {
            leadingStripped.substring(1, length - 1)
        } else {
            leadingStripped
        }
    }

    private fun String.stripQuotesDouble(): Double {
        try {
            val stripQuotes = this.stripQuotes()
            if (stripQuotes.isBlank()) {
                return 0.0
            }
            return stripQuotes.toDouble()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Error parsing '$this` to double", e)
        }
    }

    private fun Double.roundToSingleDigit() = (this * 10.0).roundToInt() / 10.0

    private fun String.toPlayerWar(): PlayerWar {
        // "1","Shohei Ohtani","https://www.fangraphs.com/players/shohei-ohtani/19755/stats","LAA","https://www.fangraphs.com/teams/angels","398","100.1","4.3","6.0"
        try {
            val elements = split(",")
            val name = elements[1].stripQuotes()
            val team = elements[3].stripQuotes()
            val pwar = elements[6].stripQuotesDouble()
            return if (pwar > 10) {
                val bwar = elements[7].stripQuotesDouble()
                PlayerWar(
                    name = name,
                    team = team,
                    bwar = bwar,
                    pwar = (elements[8].stripQuotesDouble() - bwar).roundToSingleDigit()
                )
            } else {
                PlayerWar(
                    name = name,
                    team = team,
                    bwar = pwar,
                    pwar = elements[8].stripQuotesDouble() - pwar
                )
            }
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Error parsing line: $this, message=${e.message}")
        }
    }

}
