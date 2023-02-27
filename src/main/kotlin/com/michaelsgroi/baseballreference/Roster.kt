package com.michaelsgroi.baseballreference

data class Roster(val rosterId: RosterId, val players: MutableSet<Career>)

data class RosterId(val season: Int, val team: String)