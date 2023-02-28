package com.michaelsgroi.baseballreference

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: add column headers to reports
// TODO: consolidate report name, filename, and description
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
            bestRosters(1000, concise = true),
            bestRostersByFranchise(),
            bestRostersByFranchise(concise = true),
            roster(RosterId(1928, "pha"), concise = true),
            roster(RosterId(1928, "pha")),
            roster(RosterId(2005, "nyy")),
            roster(RosterId(2005, "nyy"), concise = true),
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
            highestPeakToCareerWar(20, 10),
            highestPeakToCareerWar(20, 15),
            highestPeakToCareerWar(20, 20),
            highestPeakToCareerWar(20, 30),
            lowestPeakToCareerWar(20, 40),
            lowestPeakToCareerWar(20, 50),
            lowestPeakToCareerWar(20, 60),
            lowestPeakToCareerWar(20, 70),
            lowestPeakToCareerWar(20, 80),
            lowestPeakToCareerWar(20, 90),
            lowestPeakToCareerWar(20, 100),
            lowestPeakToCareerWar(20, 110),
            lowestPeakToCareerWar(20, 120),
            lowestPeakToCareerWar(20, 130),
            lowestPeakToCareerWar(20, 140),
            lowestPeakToCareerWar(20, 150),
            lowestPeakToCareerWar(20, 160),
            playersNameStartsWith("Cecil "),
            playersNameStartsWith("Babe "),
            playersNameStartsWith("war"),
        )
        println("running ${reports.size} reports to '$reportDir' directory")
        reports.forEach { report ->
            writeReport(report, report.run())
        }
        println("wrote ${reports.size} reports to '$reportDir' directory")
    }

    private fun playersNameStartsWith(startsWith: String) =
        Report(
            name = "Players whose name starts with $startsWith",
            filename = "playerwhosenamestartswith${startsWith.trim()}.txt",
            description = "Players who name starts with $startsWith."
        ) {
            brWarDaily.getCareers().filter { it.playerName.lowercase().startsWith(startsWith.lowercase()) }
                .sortedByDescending { it.war }.report()
        }

    private fun playersNameContains(contains: String) =
        Report(
            name = "Players whose name contains $contains",
            filename = "playerwhosenamecontainswith${contains}.txt",
            description = "Players who name contains $contains."
        ) {
            brWarDaily.getCareers().filter { it.playerName.lowercase().contains(contains.lowercase()) }
                .sortedByDescending { it.war }.report()
        }

    private fun highestPeakToCareerWar(topN: Int, maxWar: Int) =
        Report(
            name = "Highest season WAR with career WAR under $maxWar",
            filename = "highestseasonwarwithcareerwarunder${maxWar}.txt",
            description = "Highest season WAR with career WAR under $maxWar."
        ) {
            val careersUnderMaxWar =
                brWarDaily.getCareers().filter { it.war < maxWar }.associateBy { it.playerId }

            brWarDaily.getSeasons()
                .asSequence()
                .filter { careersUnderMaxWar.keys.contains(it.playerId) }.sortedByDescending { it.war }.take(topN)
                .map { careersUnderMaxWar[it.playerId]!! }.toList().report(includePeakWar = true)
        }

    private fun lowestPeakToCareerWar(topN: Int, minWar: Int) =
        Report(
            name = "Lowest season WAR with career WAR over $minWar",
            filename = "lowestseasonwarwithcareerwarover${minWar}.txt",
            description = "Lowest season WAR with career WAR over $minWar."
        ) {
            val careersOverMinWar = brWarDaily.getCareers().filter { it.war > minWar }.associateBy { it.playerId }
            val minimumMaxSeasons =
                careersOverMinWar.values.map { career -> career.seasons().maxBy { season -> season.war } }
                    .sortedBy { season -> season.war }
            val careers = minimumMaxSeasons.map { careersOverMinWar[it.playerId]!! }.take(topN)

            careers.report(includePeakWar = true)
        }


    private fun career(playerId: String) =
        Report(
            name = "$playerId career",
            filename = "${playerId}_career.txt",
            "$playerId career."
        ) {
            brWarDaily.getCareers().first { it.playerId == playerId }.seasons().toList()
                .report(includeSalary = true)
        }

    private fun highestPaidSeasons(topN: Int) =
        Report(
            name = "Highest Paid Seasons",
            filename = "highestpaidseasons.txt",
            "Highest salaries seasons."
        ) {
            brWarDaily.getSeasons().sortedByDescending { it.salary }.take(topN).report(includeSalary = true)
        }

    private fun highestPaidSeasonsOnTeam(topN: Int, team: String) =
        Report(
            name = "Highest Paid Seasons for $team",
            filename = "highestpaidseasons$team.txt",
            "Highest salaries seasons for $team."
        ) {
            brWarDaily.getSeasons().filter { it.teams.map { team -> team.lowercase() }.contains(team.lowercase()) }
                .sortedByDescending { it.salary }.take(topN).toList().report(includeSalary = true)
        }

    private fun lowestPaidWarSeasons(topN: Int) = lowestPaidWarSeasons("Lowest Paid Seasons by WAR", topN) { true }

    private fun lowestPaidWarSeasonsModernEra(topN: Int) =
        lowestPaidWarSeasons("Lowest Paid Seasons by WAR in the modern era", topN) { year -> year >= 1947 }

    private fun lowestPaidWarSeasons2000(topN: Int) =
        lowestPaidWarSeasons("Lowest Paid Seasons by WAR since 2000", topN) { year -> year >= 2000 }

    private fun lowestPaidWarSeasons(name: String, n: Int, yearFilter: (Int) -> Boolean) =
        Report(
            name = name,
            filename = name.replace(" ", "").lowercase() + ".txt",
            description = name
        ) {
            brWarDaily.getSeasons().filter { it.salary > 0 && yearFilter(it.season) }
                .sortedByDescending { it.war / it.salary }.take(n).report(includeSalary = true)
        }

    private fun lowestPaidWarCareers(topN: Int) = lowestPaidWarCareers("Lowest Paid Careers by WAR", topN) { true }

    private fun lowestPaidWarCareersModernEra(topN: Int) =
        lowestPaidWarCareers("Lowest Paid Careers by WAR in the modern era", topN) { year -> year >= 1947 }

    private fun lowestPaidWarCareers2000(topN: Int) =
        lowestPaidWarCareers("Lowest Paid Careers by WAR since 2000", topN) { year -> year >= 2000 }

    private fun lowestPaidWarCareersActive(topN: Int) =
        lowestPaidWarCareers("Lowest Paid Careers by WAR active", topN) { year -> year >= 2022 }

    private fun lowestPaidWarCareers(name: String, n: Int, yearFilter: (Int) -> Boolean) =
        Report(
            name = name,
            filename = name.replace(" ", "").lowercase() + ".txt",
            description = name
        ) {
            brWarDaily.getCareers().filter { it.salary() > 0 && yearFilter(it.seasonLines.maxOf { sl -> sl.season() }) }
                .sortedByDescending { it.war / it.salary() }.take(n).report(includeSalary = true)
        }

    private fun highestPaidByWarSeasons(topN: Int) =
        Report(
            name = "Highest Paid Seasons By WAR",
            filename = "highestpaidseasonsbywar.txt",
            "Highest $ per WAR seasons as a ratio of $ to WAR above ever lowest season WAR of -5.6."
        ) {
            val seasons = brWarDaily.getSeasons()
            val lowestWar = seasons.minOf { it.war }

            seasons.filter { it.salary > 0 }.sortedByDescending { it.salary / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)
        }

    private fun highestPaidByWarCareers(topN: Int) =
        Report(
            name = "Highest Paid Careers By WAR",
            filename = "highestpaidcareersbywar.txt",
            "Highest $ per WAR careers as a ratio of $ to WAR above ever lowest career WAR of -6.95."
        ) {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }

            careers.filter { it.salary() > 0 }.sortedByDescending { it.salary() / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)

        }

    private fun highestPaidByWarCareersActive(topN: Int) =
        Report(
            name = "Highest Paid Careers By WAR active",
            filename = "highestpaidcareersbywaractive.txt",
            "Highest $ per WAR careers as a ratio of $ to WAR above ever lowest career WAR of -6.95 amongst active " +
                "players."
        ) {
            val careers = brWarDaily.getCareers()
            val lowestWar = careers.minOf { it.war }

            careers.filter { it.salary() > 0 && it.seasonLines.maxOf { sl -> sl.season() } >= 2022 }
                .sortedByDescending { it.salary() / (it.war - lowestWar) }.take(topN)
                .report(includeSalary = true)
        }


    private fun highestPaidByNegativeWarCareers(topN: Int) =
        Report(
            name = "Highest Paid Careers with Negative WAR",
            filename = "highestpaidnegativewarcareers.txt",
            "Highest paid with negative WAR."
        ) {
            brWarDaily.getCareers().filter { it.war < 0 }.sortedByDescending { it.salary() }.take(topN)
                .report(includeSalary = true)
        }

    private fun steveBalboniAllStars() =
        Report(
            name = "Steve Balboni All-Stars",
            filename = "stevebalboniallstars.txt",
            "Players with 10 or more seasons and less than 0.5 war per season."
        ) {
            brWarDaily.getCareers().sortedBy { it.war() }
                .filter { it.seasonCount() >= 10 && (it.war() / it.seasonCount()) < 0.5 }.report()
        }

    private fun rowlandOfficeAllStars() =
        Report(
            name = "Rowland Office All-Stars",
            filename = "rowlandofficeallstars.txt",
            "Players with 10 or more seasons and negative WAR."
        ) {
            brWarDaily.getCareers().sortedBy { it.war() }.filter { it.seasonCount() >= 10 && it.war() < 0.0 }
                .report()
        }

    private fun bottomSeasonWars(): Report {
        val n = 10
        return Report(
            name = "Bottom $n Season WARs",
            filename = "bottom${n}seasonswars.txt",
            "Lowest WAR's in a single season."
        ) {
            brWarDaily.getSeasons().sortedBy { it.war }.take(10).report()
        }
    }

    private fun bestOrWorstNOfTeam(team: String, topN: Int, best: Boolean): Report {
        val bestOrWorst = if (best) "Best" else "Worst"
        return Report(
            name = "$bestOrWorst $topN of $team",
            filename = "${bestOrWorst.lowercase()}${topN}$team.txt",
            "$bestOrWorst WAR's in $team history."
        ) {
            brWarDaily.getCareers().filter {
                it.teams().map { team -> team.lowercase() }.contains(team.lowercase())
            }.sortedWith { o1, o2 ->
                if (best) {
                    o2.war().compareTo(o1.war())
                } else {
                    o1.war().compareTo(o2.war())
                }
            }.take(topN).report()
        }
    }

    private fun bestRosters(topN: Int, concise: Boolean = false) =
        Report(
            name = "Best Rosters",
            filename = "bestrosters${if (concise) "_concise" else ""}.txt",
            "Best rosters by career WAR."
        ) {
            brWarDaily.getRosters().sortedByDescending { roster -> roster.players.sumOf { it.war } }.take(topN)
                .report(concise, roundWarDecimalPlaces = 0)
        }

    private fun bestRostersByFranchise(concise: Boolean = false) =
        Report(
            name = "Best Rosters by Franchise",
            filename = "bestrostersbyfranchise${if (concise) "_concise" else ""}.txt",
            "Best rosters by career WAR by franchise."
        ) {
            val rosters = brWarDaily.getRosters()
            val topRosters =
                rosters.sortedByDescending { roster -> roster.players.sumOf { it.war } }
            val teams = rosters.map { it.rosterId.team }.distinct()

            teams.map { team -> topRosters.first { it.rosterId.team == team } }
                .sortedByDescending { roster -> roster.players.sumOf { it.war } }
                .report(concise, roundWarDecimalPlaces = 0)
        }

    private fun roster(rosterId: RosterId, concise: Boolean = false) =
        Report(
            name = "${rosterId.season} ${rosterId.team}",
            filename = "${rosterId.season}_${rosterId.team}${if (concise) "_concise" else ""}.txt",
            "${rosterId.season} ${rosterId.team} roster."
        ) {
            brWarDaily.getRosters().first { it.rosterId == rosterId }.players.sortedByDescending { it.war }
                .report(concise = concise)
        }

    private fun writeReport(report: Report, lines: List<String>) {
        val contents = lines.joinToString("\n")
        val header = "${report.name}\n${report.description}\n${now()}\n${lines.size} rows\n\n"
        val filename = report.filename
        "$reportDir/$filename".writeFile("$header$contents")
    }

    private fun List<Roster>.report(concise: Boolean = false, roundWarDecimalPlaces: Int = 2): List<String> {
        return if (concise) {
            mapIndexed { index, roster ->
                "${(index + 1)}: " + roster.report(concise, roundWarDecimalPlaces)
            }
        } else {
            val maxLength = this.size.toString().length
            mapIndexed { index, roster ->
                "${("#" + (index + 1)).padStart(maxLength + 1)}: " + roster.report(concise, roundWarDecimalPlaces)
            }
        }
    }

    private fun Roster.report(concise: Boolean = false, roundWarDecimalPlaces: Int = 2): String {
        val season = rosterId.season.toString()
        val team = rosterId.team
        val war = players.sumOf { it.war }.roundToDecimalPlaces(roundWarDecimalPlaces)
        return if (concise) {
            "$season $team ${war.toInt()}"
        } else {
            season.padEnd(5) + team.padEnd(4) + war.toString().padStart(7)
        }
    }

    private fun List<Career>.report(
        concise: Boolean = false,
        includeSalary: Boolean = false,
        includePeakWar: Boolean = false
    ): List<String> {
        val maxLength = this.size.toString().length
        return this.mapIndexed { index, career ->
            val prefixNew = if (concise) {
                "${(index + 1)}: "
            } else {
                "${("#" + (index + 1)).padStart(maxLength + 1)}: "
            }
            prefixNew + career.report(
                concise,
                includeSalary,
                includePeakWar
            )
        }
    }

    private fun Career.report(
        concise: Boolean = false,
        includeSalary: Boolean = false,
        includePeakWar: Boolean = false
    ): String {
        if (concise) {
            return "$playerName ${war().roundToDecimalPlaces(0).toInt()}"
        }
        return this.playerName.padEnd(24) +
            war().toString().padStart(7) +
            (if (includePeakWar) {
                val peakSeason = seasons().maxBy { it.war }
                ("${peakSeason.war.roundToDecimalPlaces(1)} (${peakSeason.season})").padStart(15)
            } else "") +
            (if (includeSalary) salary().toString().padStart(15) else "") +
            seasonCount().toString().padStart(3) +
            (" (" + seasonRange() + ") ").padEnd(12) +
            teams().joinToString(",")
    }

    private fun List<Season>.report(includeSalary: Boolean = false): List<String> {
        val maxLength = this.size.toString().length
        return this.mapIndexed { index, career ->
            "${("#" + (index + 1)).padStart(maxLength + 1)}: " + career.report(
                includeSalary
            )
        }
    }

    private fun Season.report(includeSalary: Boolean = false): String =
        playerName.padEnd(24) +
            war.roundToDecimalPlaces(2).toString().padStart(7) +
            (if (includeSalary) salary.toString().padStart(15) else "") +
            season.toString().padStart(5) + " " +
            teams.joinToString(",")

    class Report(
        val name: String,
        val filename: String,
        val description: String? = null,
        val run: () -> List<String>
    )

    private fun now(): String? {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return formatter.format(now)
    }

}