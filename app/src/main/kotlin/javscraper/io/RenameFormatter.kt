package javscraper.io

import javscraper.models.Video

object RenameFormatter {

    fun formatFolder(video: Video, layers: List<String>, suffix: String): List<String> {
        return layers.map { sanitize(formatTemplate(it, video, suffix, useFallback = true)) }
            .filter { it.isNotBlank() }
    }

    fun formatFilename(
        video: Video, template: String, suffix: String,
        maxFileLen: Int, maxTitleLen: Int
    ): String {
        val result = formatTemplate(template, video, suffix, useFallback = false, maxTitleLen = maxTitleLen)
        return truncate(sanitize(result), maxFileLen)
    }

    fun detectSuffix(originalFileName: String, keywords: List<String>): String {
        val lower = originalFileName.lowercase()
        return keywords.filter { kw ->
            val kwl = kw.lowercase().trim()
            kwl.isNotBlank() && Regex(Regex.escape(kwl) + "(?=[-_.\\s]|\$)").containsMatchIn(lower)
        }.joinToString("")
    }

    private fun formatTemplate(
        template: String, video: Video, suffix: String,
        useFallback: Boolean, maxTitleLen: Int = Int.MAX_VALUE
    ): String {
        var result = template
        result = result.replace("{num}", video.number)
        val cleanedTitle = truncate(cleanSourceSuffix(video.title), maxTitleLen)
        result = result.replace("{title}", cleanedTitle)
        result = result.replace("{actor}", video.actresses.firstOrNull() ?: "")
        result = result.replace("{actors}", video.actresses.joinToString(" "))
        result = result.replace("{maker}", video.maker)
        result = result.replace("{label}", video.label)
        result = result.replace("{series}", video.series)
        result = result.replace("{director}", video.director)
        result = result.replace("{date}", video.date)
        result = result.replace("{year}", if (video.date.length >= 4) video.date.substring(0, 4) else "")
        result = result.replace("{month}", if (video.date.length >= 7) video.date.substring(5, 7) else "")
        result = result.replace("{day}", if (video.date.length >= 10) video.date.substring(8, 10) else "")
        result = result.replace("{suffix}", suffix)
        return result.trim()
    }

    fun sanitize(name: String): String {
        return name.replace(illegalCharsRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
            .let { stripWindowsTrailing(it) }
    }

    fun truncate(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        if (maxLen <= 3) return text.take(maxLen)
        return text.take(maxLen - 3).trimEnd() + "..."
    }

    fun stripWindowsTrailing(name: String): String {
        return if (System.getProperty("os.name").lowercase().contains("win")) {
            name.trimEnd('.', ' ')
        } else name
    }

    /** Strip detected suffix from the end of a formatted name, returning the base name for shared assets (NFO/images). */
    fun stripPartSuffix(name: String, suffix: String): String {
        if (suffix.isBlank() || !name.endsWith(suffix)) return name
        return name.dropLast(suffix.length).trimEnd()
    }

    fun cleanSourceSuffix(text: String): String {
        if (text.isBlank()) return text
        var result = text
        for (p in sourceSuffixPatterns) {
            result = p.replaceFirst(result, "")
        }
        return result.trim()
    }
}

private val illegalCharsRegex = """[<>:"/\\|?*]""".toRegex()
private val multiSpaceRegex = """\s+""".toRegex()

private val sourceSuffixPatterns = listOf(
    Regex("""\s*-\s*Jable\s*TV.*$""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*Jable.*$""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*Hayav\s*AV.*$""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*Hayav.*$""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*MissAV.*$""", RegexOption.IGNORE_CASE),
    Regex("""\s*-\s*$"""),
    Regex("""\s+-\d+$"""),
)
