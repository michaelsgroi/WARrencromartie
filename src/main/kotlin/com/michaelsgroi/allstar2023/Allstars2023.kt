package com.michaelsgroi.allstar2023

import com.michaelsgroi.baseballreference.readFile

class AllStars2023(private val filename: String) {

    fun getAllStars() = deserializeToAllStar(filename.readFile())

    private fun deserializeToAllStar(allStarCsvLines: List<String>) =
        allStarCsvLines.map { line ->
            // C: Jonah Heim (TEX)
            val (position, nameAndTeam) = line.split(":")
            val (nameUntrimmed, teamWithParans) = nameAndTeam.split("(")
            val name = nameUntrimmed.trim()
            val team = teamWithParans.substringBefore(")")
            AllStar(position, name, team)
        }

    data class AllStar(
        val position: String,
        val name: String,
        val team: String
    )

}
