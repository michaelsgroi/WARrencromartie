package com.michaelsgroi.baseballreference

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BrReports(
    private val brWarDaily: BrWarDaily,
    private val reportDir: String = "reports"
) {
    init {
        reportDir.createDirectoryIfNotExists()
    }

    fun run() {
        val reports = listOf(
            steveBalboniAllStars(),
            rowlandOfficeAllStars(),
            bottomSeasonWars(),
            bestOrWorstNOfTeam("bos", 30, true),
            bestOrWorstNOfTeam("bos", 30, false),
            bestOrWorstNOfTeam("nyy", 30, true),
            bestOrWorstNOfTeam("nyy", 30, false),
            bestRosters(1000),
            bestRostersByFranchise(),
            roster(RosterId(2005, "nyy")),
            roster(RosterId(1959, "mln")),
            roster(RosterId(1996, "cle")),
            highestPaidSeasons(20),
            highestPaidSeasonsOnTeam(20, "bos"),
            highestPaidSeasonsOnTeam(20, "nyy"),
            highestPaidByWarSeasons(20),
            highestPaidByWarCareers(20),
            highestPaidByWarCareersActive(20),
            highestPaidByNegativeWarCareers(20),
            lowestPaidWarSeasons(20),
            lowestPaidWarSeasonsModernEra(20),
            lowestPaidWarSeasons2000(20),
            lowestPaidWarCareers(20),
            lowestPaidWarCareersModernEra(20),
            lowestPaidWarCareers2000(20),
            lowestPaidWarCareersActive(20),
            career("ruthba01"),
//            highestPeakToCareerWar(20),
//            lowestPeakToCareerWar(20)
        )
        println("running ${reports.size} reports to '$reportDir' directory")
        reports.forEach { report ->
            writeReport(report, report.run())
        }
        println("wrote ${reports.size} reports to '$reportDir' directory")
    }

//    private fun highestPeakToCareerWar(n: Int) = object : Report(
//        name = "Highest peak to career WAR",
//        filename = "highestpeaktocareerwar.txt",
//        description = "Highest peak to career WAR.") {
//            override fun run(): List<String> {
//                val wars = brWarDaily.getSeasons().map {it.war}.sortedBy { it }
//                return brWarDaily.getCareers().sortedByDescending { career ->
//                    val peakSeasonPerc = career.seasons().maxOf { wars.percentile(it.war) }
//                    val careerPerc = career.warPercentile
//                    peakSeasonPerc / careerPerc
//                }.map { it.report(includePercentiles = true) }
//            }
//    }

//    private fun lowestPeakToCareerWar(n: Int) = object : Report(
//        name = "Lowest peak to career WAR",
//        filename = "lowestpeaktocareerwar.txt",
//        description = "Lowest peak to career WAR.") {
//        override fun run(): List<String> {
//            val wars = brWarDaily.getSeasons().map {it.war}.sortedBy { it }
//            return brWarDaily.getCareers().sortedBy { career ->
//                val peakSeasonPerc = career.seasons().maxOf { wars.percentile(it.war) }
//                val careerPerc = career.warPercentile
//                peakSeasonPerc / careerPerc
//            }.map { it.report(includePercentiles = true) }
//        }
//    }

//    private fun List<Double>.percentile(value: Double): Double {
//        return (indexOf(value).toDouble() / this.size.toDouble()) * 100
//    }

    private fun career(playerId: String) = object : Report(
        name = "$playerId career",
        filename = "${playerId}_career.txt",
        "$playerId career."
    ) {
        override fun run(): List<String> {
            return brWarDaily.getCareers().first { it.playerId == playerId }.seasons()
                .map { it.report(includeSalary = true) }
        }
    }

    private fun highestPaidSeasons(n: Int) = object : Report(
        name = "Highest Paid Seasons",
        filename = "highestpaidseasons.txt",
        "Highest salaries seasons."
    ) {
        override fun run() =
            brWarDaily.getSeasons().sortedByDescending { it.salary }.take(n).map { it.report(includeSalary = true) }
    }

    private fun highestPaidSeasonsOnTeam(n: Int, team: String) = object : Report(
        name = "Highest Paid Seasons for $team",
        filename = "highestpaidseasons$team.txt",
        "Highest salaries seasons for $team."
    ) {
        override fun run() =
            brWarDaily.getSeasons().filter { it.teams.map { team -> team.lowercase() }.contains(team.lowercase()) }.sortedByDescending { it.salary }.take(n).map { it.report(includeSalary = true) }
    }

    private fun lowestPaidWarSeasons(n: Int) = lowestPaidWarSeasons("Lowest Paid Seasons by WAR", n) { true }

    private fun lowestPaidWarSeasonsModernEra(n: Int) = lowestPaidWarSeasons("Lowest Paid Seasons by WAR in the modern era", n) { year -> year >= 1947 }

    private fun lowestPaidWarSeasons2000(n: Int) = lowestPaidWarSeasons("Lowest Paid Seasons by WAR since 2000", n) { year -> year >= 2000 }

    private fun lowestPaidWarSeasons(name: String, n: Int, yearFilter: (Int) -> Boolean) = object : Report(
        name = name,
        filename = name.replace(" ", "").lowercase() + ".txt",
        description = name
    ) {
        override fun run() =
            brWarDaily.getSeasons().filter { it.salary > 0 && yearFilter(it.season) }.sortedByDescending { it.war / it.salary }.take(n).map { it.report(includeSalary = true) }
    }

    private fun lowestPaidWarCareers(n: Int) = lowestPaidWarCareers("Lowest Paid Careers by WAR", n) { true }

    private fun lowestPaidWarCareersModernEra(n: Int) = lowestPaidWarCareers("Lowest Paid Careers by WAR in the modern era", n) { year -> year >= 1947 }

    private fun lowestPaidWarCareers2000(n: Int) = lowestPaidWarCareers("Lowest Paid Careers by WAR since 2000", n) { year -> year >= 2000 }

    private fun lowestPaidWarCareersActive(n: Int) = lowestPaidWarCareers("Lowest Paid Careers by WAR active", n) { year -> year >= 2022 }

    private fun lowestPaidWarCareers(name: String, n: Int, yearFilter: (Int) -> Boolean) = object : Report(
        name = name,
        filename = name.replace(" ", "").lowercase() + ".txt",
        description = name
    ) {
        override fun run() =
            brWarDaily.getCareers().filter { it.salary() > 0 && yearFilter(it.seasonLines.maxOf { sl -> sl.season() }) }.sortedByDescending { it.war / it.salary() }.take(n).map { it.report(includeSalary = true) }
    }

    private fun highestPaidByWarSeasons(n: Int) = object : Report(
        name = "Highest Paid Seasons By WAR",
        filename = "highestpaidseasonsbywar.txt",
        "Highest $ per WAR seasons as a ratio of $ to WAR above ever lowest season WAR of -5.6."
    ) {
        override fun run(): List<String> {
            val seasons = brWarDaily.getSeasons()
            val lowestWar = seasons.minOf { it.war }
            return seasons.filter { it.salary > 0 }.sortedByDescending { it.salary / (it.war - lowestWar) }.take(n)
                .map { it.report(includeSalary = true) }
        }
    }

    private fun highestPaidByWarCareers(n: Int) = object : Report(
        name = "Highest Paid Careers By WAR",
        filename = "highestpaidcareersbywar.txt",
        "Highest $ per WAR careers as a ratio of $ to WAR above ever lowest career WAR of -6.95."
    ) {
        override fun run(): List<String> {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }
            return careers.filter { it.salary() > 0 }.sortedByDescending { it.salary() / (it.war - lowestWar) }.take(n)
                .map { it.report(includeSalary = true) }
        }
    }

    private fun highestPaidByWarCareersActive(n: Int) = object : Report(
        name = "Highest Paid Careers By WAR active",
        filename = "highestpaidcareersbywaractive.txt",
        "Highest $ per WAR careers as a ratio of $ to WAR above ever lowest career WAR of -6.95 amongst active players."
    ) {
        override fun run(): List<String> {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }
            return careers.filter { it.salary() > 0 && it.seasonLines.maxOf { sl -> sl.season() } >= 2022 }.sortedByDescending { it.salary() / (it.war - lowestWar) }.take(n)
                .map { it.report(includeSalary = true) }
        }
    }


    private fun highestPaidByNegativeWarCareers(n: Int) = object : Report(
        name = "Highest Paid Careers with Negative WAR",
        filename = "highestpaidnegativewarcareers.txt",
        "Highest paid with negative WAR."
    ) {
        override fun run(): List<String> {
            val careers = brWarDaily.getCareers()
            return careers.filter { it.war < 0 }.sortedByDescending { it.salary() }.take(n)
                .map { it.report(includeSalary = true) }
        }
    }

    private fun steveBalboniAllStars() =
        object : Report(
            name = "Steve Balboni All-Stars",
            filename = "stevebalboniallstars.txt",
            "Players with 10 or more seasons and less than 0.5 war per season."
        ) {
            override fun run() =
                brWarDaily.getCareers().sortedBy { it.war() }
                    .filter {
                        it.seasonCount() >= 10
                                && (it.war() / it.seasonCount()) < 0.5
                    }.map { it.report() }
        }

    private fun rowlandOfficeAllStars() =
        object : Report(
            name = "Rowland Office All-Stars",
            filename = "rowlandofficeallstars.txt",
            "Players with 10 or more seasons and negative WAR."
        ) {
            override fun run() =
                brWarDaily.getCareers().sortedBy { it.war() }
                    .filter { it.seasonCount() >= 10 && it.war() < 0.0 }.map { it.report() }
        }

    private fun bottomSeasonWars(): Report {
        val n = 10
        return object : Report(
            name = "Bottom $n Season WARs",
            filename = "bottom${n}seasonswars.txt",
            "Lowest WAR's in a single season."
        ) {
            override fun run() =
                brWarDaily.getSeasons().sortedBy { it.war }.take(10).map { it.report() }
        }
    }

    private fun bestOrWorstNOfTeam(team: String, n: Int, best: Boolean): Report {
        val bestOrWorst = if (best) "Best" else "Worst"
        return object : Report(
            name = "$bestOrWorst $n of $team",
            filename = "${bestOrWorst.lowercase()}${n}$team.txt",
            "$bestOrWorst WAR's in $team history."
        ) {
            override fun run(): List<String> {
                return brWarDaily.getCareers().filter {
                    it.teams().map { team -> team.lowercase() }.contains(team.lowercase())
                }.sortedWith { o1, o2 ->
                    if (best) {
                        o2.war().compareTo(o1.war())
                    } else {
                        o1.war().compareTo(o2.war())
                    }
                }.map { it.report() }.take(n)
            }
        }
    }

    private fun bestRosters(n: Int) =
        object : Report(
            name = "Best Rosters",
            filename = "bestrosters.txt",
            "Best rosters by career WAR."
        ) {
            override fun run(): List<String> {
                return brWarDaily.getRosters().sortedByDescending { roster -> roster.players.sumOf { it.war } }.take(n)
                    .map { it.report(roundWarDecimalPlaces = 0) }
            }
        }

    private fun bestRostersByFranchise() =
        object : Report(
            name = "Best Rosters by Franchise",
            filename = "bestrostersbyfranchise.txt",
            "Best rosters by career WAR by franchise."
        ) {
            override fun run(): List<String> {
                val rosters = brWarDaily.getRosters()
                val topRosters =
                    rosters.sortedByDescending { roster -> roster.players.sumOf { it.war } }
                val teams = rosters.map { it.rosterId.team }.distinct()
                return teams.map { team -> topRosters.first { it.rosterId.team == team } }.sortedByDescending { roster -> roster.players.sumOf { it.war } }.map { it.report(roundWarDecimalPlaces = 0) }
            }
        }

    private fun roster(rosterId: RosterId) =
        object : Report(
            name = "${rosterId.season} ${rosterId.team}",
            filename = "${rosterId.season}_${rosterId.team}.txt",
            "${rosterId.season} ${rosterId.team} roster."
        ) {
            override fun run() =
                brWarDaily.getRosters().first { it.rosterId == rosterId }.players.sortedByDescending { it.war }
                    .map { it.report() }
        }

    private fun writeReport(report: Report, lines: List<String>) {
        val contents = lines.joinToString("\n")
        val header = "${report.name}\n${report.description}\n${now()}\n${lines.size} rows\n\n"
        val filename = report.filename
        "$reportDir/$filename".writeFile("$header$contents")
    }

    private fun Roster.report(roundWarDecimalPlaces: Int = 2): String =
        rosterId.season.toString().padEnd(5) +
                rosterId.team.padEnd(4) +
                players.sumOf { it.war }.roundToDecimalPlaces(roundWarDecimalPlaces).toString().padStart(7)

    private fun Career.report(includeSalary: Boolean = false, includePercentiles: Boolean = false): String =
        this.playerName.padEnd(24) +
                war().toString().padStart(7) +
                (if (includePercentiles) ("${warPercentile.roundToDecimalPlaces(1)}%").padStart(5) else "") +
                (if (includeSalary) salary().toString().padStart(15) else "") +
                seasonCount().toString().padStart(3) +
                (" (" + seasonRange() + ") ").padEnd(12) +
                teams().joinToString(",")

    private fun Season.report(includeSalary: Boolean = false): String =
        playerName.padEnd(24) +
                war.roundToDecimalPlaces(2).toString().padStart(7) +
                (if (includeSalary) salary.toString().padStart(15) else "") +
                season.toString().padStart(5) + " " +
                teams.joinToString(",")

    abstract class Report(
        val name: String,
        val filename: String,
        val description: String? = null
    ) {
        abstract fun run(): List<String>
    }

    private fun now(): String? {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return formatter.format(now)
    }

}