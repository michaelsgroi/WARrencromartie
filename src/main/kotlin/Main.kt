import com.michaelsgroi.baseballreference.BrReports
import com.michaelsgroi.baseballreference.BrWarDaily
import com.michaelsgroi.baseballreference.WarParquet
import com.michaelsgroi.baseballreference.WarReports

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "parquet" -> {
            BrWarDaily(expiration = java.time.Duration.ofHours(24)).seasons // force CSV download
            WarParquet.generate()
        }
        "warreports" -> WarReports.run()
        else -> {
            BrReports(BrWarDaily()).run()
            WarReports.run()
        }
    }
}

