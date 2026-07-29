package javscraper.io

import javscraper.models.ScannedFile
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object FileScanner {
    private val log = KotlinLogging.logger {}
    private val VIDEO_EXTS = setOf(
        "mp4", "mkv", "avi", "ts", "m2ts", "wmv", "flv", "mov",
        "webm", "vob", "m4v", "3gp", "mpg", "mpeg", "rm", "rmvb"
    )

    // Special prefixes that sometimes appear without a dash before the number, e.g. FC2-123456 or fc2ppv-123
    private val SPECIAL_PREFIXES = arrayOf(
        "fc2", "heyzo", "siro", "carib", "10mu", "1pon",
        "mukd", "paco", "toky", "gano"
    )

    // Build RX1 dynamically: allow optional dash (and "ppv-" for FC2) between prefix and digits
    private val SPECIAL_ALTERNATION = SPECIAL_PREFIXES.joinToString("|") { pfx ->
        if (pfx == "fc2") "(?:${pfx}-?(?:ppv-?)?\\d[\\d\\-]*)"
        else "(?:${pfx}-?\\d[\\d\\-]*)"
    }
    private val RX1 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,])((?:$SPECIAL_ALTERNATION))",
        RegexOption.IGNORE_CASE
    )

    // Standard JAV: 2-6 letters, optional separator, 2-5 digits
    private val RX2 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,])([A-Za-z]{2,6}[-–]\\d{2,5})",
        RegexOption.IGNORE_CASE
    )

    // Pure-numeric codes: 6 digits, separator, 2-4 digits
    private val RX3 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,])(\\d{6}[-–]\\d{2,4})"
    )

    private val EXCLUDE = setOf(
        "sample", "trailer", "screenshot", "thumb", "cover",
        "poster", "fanart", "extra", "sub", "subtitle",
        "1080p", "720p", "4k", "h264", "h265", "x264", "x265"
    )

    fun scanDirectory(dir: Path, recursive: Boolean = true): List<ScannedFile> {
        if (!Files.isDirectory(dir)) return emptyList()
        val s = if (recursive) Files.walk(dir) else Files.list(dir)
        return try {
            s.use {
                it.filter { Files.isRegularFile(it) }.filter { isVideo(it) }.map { f ->
                    ScannedFile(
                        f.toAbsolutePath().toString(),
                        f.fileName.toString(),
                        extractNumber(f.fileName.toString())
                    )
                }.toList()
            }
        } catch (e: Exception) {
            log.error(e) { "scan error" }; emptyList()
        }
    }

    fun isVideo(p: Path): Boolean =
        p.fileName.toString().substringAfterLast(".", "").lowercase() in VIDEO_EXTS

    fun isSample(n: String): Boolean =
        EXCLUDE.any { n.lowercase().contains(it) }

    fun extractNumber(fileName: String): String {
        val n = fileName.substringBeforeLast(".")
        if (isSample(fileName)) return ""

        for (r in listOf(RX1, RX2, RX3)) {
            val m = r.find(n)
            if (m != null) return m.groupValues[1].replace("–", "-").uppercase()
        }
        return ""
    }
}
