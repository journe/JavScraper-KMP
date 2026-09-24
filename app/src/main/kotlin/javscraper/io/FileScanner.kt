package javscraper.io

import javscraper.models.ScannedFile
import javscraper.settings.MultiPartSuffix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path

data class FileNameInfo(
    val number: String,
    val versionLabel: String = "",
    val version: String = ""
)

object FileScanner {
    private val log = KotlinLogging.logger {}
    private val VIDEO_EXTS = setOf(
        "mp4", "mkv", "avi", "ts", "m2ts", "wmv", "flv", "mov",
        "webm", "vob", "m4v", "3gp", "mpg", "mpeg", "rm", "rmvb"
    )

    // Special prefixes that may appear with or without a dash before the number
    private val SPECIAL_PREFIXES = arrayOf(
        "fc2", "heyzo", "siro", "carib", "10mu", "1pon",
        "mukd", "paco", "toky", "gano"
    )

    // Build RX1 dynamically for special prefixes other than FC2.
    private val SPECIAL_ALTERNATION = SPECIAL_PREFIXES
        .filterNot { it == "fc2" }
        .joinToString("|") { pfx -> "(?:${pfx}-?\\d+)" }

    // FC2 numbering commonly appears as FC2-123, FC2-PPV-123 or FC2-PPV 123.
    // The optional trailing group is a multi-file label, such as -2 or -4k.
    private val RX_FC2 = Regex(
        """(?:^|[\s\-_\[\(.,@])FC2(?:[-_ ]?PPV)?[-_ ]?(\d+)(?:[-_. ]+(.+))?(?=$|[\s\-_\[\]().,@])""",
        RegexOption.IGNORE_CASE
    )

    private val RX1 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,@])((?:$SPECIAL_ALTERNATION))(?:[-_. ]+(.+))?(?=$|[\\s\\-_\\[\\]().,@])",
        RegexOption.IGNORE_CASE
    )

    // Standard JAV: 2-6 letters, separator, 2-5 digits
    private val RX2 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,@])([A-Za-z]{2,6}[-–]\\d{2,5})(?:[-_. ]+(.+))?(?=$|[\\s\\-_\\[\\]().,@])",
        RegexOption.IGNORE_CASE
    )

    // Numeric codes: 6 digits, separator (dash/en-dash/underscore), 2-4 digits
    // Covers both 123456-789 and 011225_01 / 031226_001 formats
    private val RX3 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,@])(\\d{6}[-–_]\\d{2,4})(?:[-_. ]+(.+))?(?=$|[\\s\\-_\\[\\]().,@])"
    )

    // Digit-prefixed studio codes: 1-4 digits, 2-6 letters, separator, 2-5 digits (e.g. 476MLA-192)
    private val RX4 = Regex(
        "(?:^|[\\s\\-_\\[\\(.,@])(\\d{1,4}[A-Za-z]{2,6}[-–]\\d{2,5})(?:[-_. ]+(.+))?(?=$|[\\s\\-_\\[\\]().,@])",
        RegexOption.IGNORE_CASE
    )

    private val EXCLUDE = setOf(
        "sample", "trailer", "screenshot", "thumb", "cover",
        "poster", "fanart", "extra", "sub", "subtitle"
    )

    private val versionTokenRegex = Regex("""(?i)^[a-z]*\d+[a-z]*$""")
    private val specialVersionLetters = setOf("C", "U")

    fun scanDirectory(dir: Path, recursive: Boolean = true): List<ScannedFile> {
        if (!Files.isDirectory(dir)) return emptyList()
        val stream = if (recursive) Files.walk(dir) else Files.list(dir)
        return try {
            stream.use {
                val files = it
                    .filter { file -> Files.isRegularFile(file) }
                    .filter { file -> isVideo(file) }
                    .iterator()
                val result = ArrayList<ScannedFile>()
                while (files.hasNext()) result.add(toScannedFile(files.next()))
                result
            }
        } catch (e: Exception) {
            log.error(e) { "scan error" }; emptyList()
        }
    }

    fun scanDirectoryFlow(
        dir: Path,
        recursive: Boolean = true,
        batchSize: Int = 50
    ): Flow<List<ScannedFile>> = flow {
        if (!Files.isDirectory(dir)) return@flow
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        val stream = if (recursive) Files.walk(dir) else Files.list(dir)
        try {
            stream.use {
                var batch = ArrayList<ScannedFile>(effectiveBatchSize)
                val files = it
                    .filter { file -> Files.isRegularFile(file) }
                    .filter { file -> isVideo(file) }
                    .iterator()
                while (files.hasNext()) {
                    val file = files.next()
                    batch.add(toScannedFile(file))
                    if (batch.size == effectiveBatchSize) {
                        emit(batch)
                        batch = ArrayList(effectiveBatchSize)
                    }
                }
                if (batch.isNotEmpty()) emit(batch)
            }
        } catch (e: Exception) {
            log.error(e) { "scan error" }
        }
    }

    private fun toScannedFile(file: Path): ScannedFile {
        val nfoFile = findMatchingNfo(file)
        return ScannedFile(
            file.toAbsolutePath().toString(),
            file.fileName.toString(),
            extractNumber(file.fileName.toString()),
            nfoFile != null,
            nfoFile?.let(NfoReader::read)
        )
    }

    fun findMatchingNfo(video: Path): Path? {
        val parent = video.parent
        val movieNfo = parent?.resolve("movie.nfo")
        if (movieNfo != null && Files.isRegularFile(movieNfo)) return movieNfo

        val videoName = video.fileName.toString()
        val nfoName = videoName.substringBeforeLast('.', videoName) + ".nfo"
        val nfo = video.resolveSibling(nfoName)
        if (Files.isRegularFile(nfo)) return nfo

        val sharedNfo = parent?.resolve(parent.fileName.toString() + ".nfo")
        return sharedNfo?.takeIf { Files.isRegularFile(it) }
    }

    fun isVideo(p: Path): Boolean =
        p.fileName.toString().substringAfterLast(".", "").lowercase() in VIDEO_EXTS

    fun isMultiPartLabel(label: String): Boolean =
        label.trim().matches(multiPartLabelRegex)

    fun multiPartLabelNumber(label: String): Int? {
        val groups = multiPartLabelNumberRegex.matchEntire(label.trim())?.groupValues ?: return null
        return groups.drop(1).firstOrNull { it.isNotEmpty() }?.toIntOrNull()
    }

    /** 多段视频中，第一段或没有分段后缀的主文件使用 movie.nfo 作为主元数据。 */
    fun isPrimaryPart(fileName: String, directory: Path?): Boolean {
        val label = parseFileName(fileName).versionLabel
        val partNumber = multiPartLabelNumber(label)
        val movieNfo = directory?.resolve("movie.nfo")
        return partNumber == 1 ||
            (label.isBlank() && movieNfo != null && Files.isRegularFile(movieNfo))
    }

    fun isSample(n: String): Boolean =
        EXCLUDE.any { n.lowercase().contains(it) }

    fun extractNumber(fileName: String): String {
        return parseFileName(fileName).number
    }

    fun parseFileName(fileName: String): FileNameInfo {
        val n = fileName.substringBeforeLast(".")
        if (isSample(fileName)) return FileNameInfo("")

        RX_FC2.find(n)?.let { m ->
            val trailing = parseTrailingTokens(parseTrailingText(m, n))
            return FileNameInfo(
                number = "FC2-${m.groupValues[1]}",
                versionLabel = trailing.label,
                version = trailing.version
            )
        }
        for (r in listOf(RX1, RX2, RX3, RX4)) {
            val m = r.find(n)
            if (m != null) {
                val trailing = parseTrailingTokens(parseTrailingText(m, n))
                return FileNameInfo(
                    number = m.groupValues[1].replace("–", "-").uppercase(),
                    versionLabel = trailing.label,
                    version = trailing.version
                )
            }
        }
        return FileNameInfo("")
    }

    private fun parseTrailingText(m: MatchResult, n: String): String {
        m.groupValues[2].takeIf { it.isNotBlank() }?.let { return it }
        return extractBracketTrailing(n, m.range.last + 1)
    }

    private fun extractBracketTrailing(n: String, afterNumberIndex: Int): String {
        if (afterNumberIndex >= n.length) return ""
        val closing = n[afterNumberIndex]
        if (closing != ']' && closing != ')' && closing != '）') return ""
        val rest = n.substring(afterNumberIndex + 1).trim()
        val last = rest.split(trailingSeparatorRegex).lastOrNull { it.isNotBlank() } ?: return ""
        return if (isPlausibleLabel(last)) last else ""
    }

    private fun isPlausibleLabel(token: String): Boolean {
        if (token.isBlank() || token.length > 8) return false
        if (token.matches(versionTokenRegex)) return true
        return token.length == 1 && token.uppercase() in specialVersionLetters
    }

    private fun parseTrailingTokens(value: String): TrailingTokens {
        val tokens = value.split(trailingSeparatorRegex)
            .map(String::trim)
            .filter { it.isNotBlank() }
        val version = buildString {
            if (tokens.any { isSpecialVersionToken(it, "C") }) append('C')
            if (tokens.any { isSpecialVersionToken(it, "U") }) append('U')
        }
        val label = tokens
            .filterNot { isSpecialVersionToken(it, "C") || isSpecialVersionToken(it, "U") }
            .joinToString(" ")
        return TrailingTokens(label, version)
    }

    private fun isSpecialVersionToken(token: String, expected: String): Boolean =
        token.length == 1 && token.equals(expected, ignoreCase = true)

    private data class TrailingTokens(
        val label: String,
        val version: String
    )
}

private val multiPartTypes = MultiPartSuffix.entries.joinToString("|") { it.value }
private val multiPartLabelRegex = Regex("""(?i)^(?:$multiPartTypes)[ _.-]*[0-9]+$|^[1-9]$""")
private val multiPartLabelNumberRegex = Regex("""(?i)^(?:$multiPartTypes)[ _.-]*([0-9]+)$|^([1-9])$""")

private val trailingSeparatorRegex = Regex("""[-_.\s]+""")
