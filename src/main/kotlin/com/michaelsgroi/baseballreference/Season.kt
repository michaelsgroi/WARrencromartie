package com.michaelsgroi.baseballreference

import com.michaelsgroi.baseballreference.BrWarDaily.*

data class Season(
    val playerId: String,
    val playerName: String,
    val season: Int,
    val teams: Set<String>,
    val leagues: Set<String>,
    val seasons: Set<SeasonType>,
    val war: Double,
    val salary: Long,
    val battingWar: Double,
    val pitchingWar: Double
)