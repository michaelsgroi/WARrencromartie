import com.michaelsgroi.baseballreference.BrReports
import com.michaelsgroi.baseballreference.BrWarDaily
import com.michaelsgroi.baseballreference.LahmanDownloader
import com.michaelsgroi.baseballreference.Parqlo
import com.michaelsgroi.baseballreference.RetroDataLoader
import com.michaelsgroi.baseballreference.WarParquet
import java.time.Duration

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "br"         -> BrWarDaily.downloadAll(Duration.ofHours(24))
        "lahman"     -> LahmanDownloader.download()
        "retrosheet" -> RetroDataLoader.download()
        "parquet" -> {
            RetroDataLoader.writeParquets()
            WarParquet.generate()
        }
        "warreports" -> BrReports.run(args.getOrNull(1))
        "parqlo" -> Parqlo.generate(args.getOrNull(1) ?: Parqlo.DEFAULT_TARGET)
        else -> BrReports.run()
    }
}
