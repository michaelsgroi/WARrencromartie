import com.michaelsgroi.baseballreference.BrReports
import com.michaelsgroi.baseballreference.BrWarDaily
import com.michaelsgroi.baseballreference.Parqlo
import com.michaelsgroi.baseballreference.WarParquet

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "parquet" -> {
            BrWarDaily(expiration = java.time.Duration.ofHours(24)).seasons // force CSV download
            WarParquet.generate()
        }
        "warreports" -> BrReports.run(args.getOrNull(1))
        "parqlo" -> Parqlo.generate(args.getOrNull(1) ?: Parqlo.DEFAULT_TARGET)
        else -> BrReports.run()
    }
}
