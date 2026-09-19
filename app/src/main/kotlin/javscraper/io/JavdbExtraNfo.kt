package javscraper.io

import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video
import javscraper.models.VideoUpdateField
import javscraper.models.VideoCollectionMerger
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.stream.XMLStreamWriter

internal data class JavdbExtra(
    val wantCount: Int? = null,
    val watchedCount: Int? = null,
    val ratingCount: Int? = null,
    val rankings: List<Ranking> = emptyList(),
    val reviews: List<Review> = emptyList()
)

internal object JavdbExtraNfo {
    private val blockPattern = Regex(
        """(?s)\r?\n[ \t]*<javdb_extra(?:\s[^>]*)?>.*?</javdb_extra>"""
    )

    fun write(writer: XMLStreamWriter, video: Video) {
        if (!video.hasJavdbExtra()) return
        writer.writeCharacters("\n  ")
        writer.writeStartElement("javdb_extra")
        video.wantCount?.let { write(writer, "want_count", it.toString(), "    ") }
        video.watchedCount?.let { write(writer, "watched_count", it.toString(), "    ") }
        video.ratingCount?.let { write(writer, "rating_count", it.toString(), "    ") }
        video.rankings.forEach { writeRanking(writer, it) }
        video.reviews.forEach { writeReview(writer, it) }
        writer.writeCharacters("\n  ")
        writer.writeEndElement()
    }

    fun read(document: Document): JavdbExtra {
        val root = document.getElementsByTagName("javdb_extra")
            .item(0) as? Element ?: return JavdbExtra()
        return JavdbExtra(
            wantCount = root.childText("want_count")?.trim()?.toIntOrNull(),
            watchedCount = root.childText("watched_count")?.trim()?.toIntOrNull(),
            ratingCount = root.childText("rating_count")?.trim()?.toIntOrNull(),
            rankings = root.childElements("ranking").mapNotNull(::readRanking),
            reviews = root.childElements("review").mapNotNull(::readReview)
        )
    }

    fun update(original: String, video: Video): String {
        val match = blockPattern.find(original)
        val newline = if (original.contains("\r\n")) "\r\n" else "\n"
        val block = render(video, newline)
        if (match != null) {
            return if (block == null) {
                original.removeRange(match.range)
            } else {
                original.replaceRange(match.range, newline + block)
            }
        }
        if (block == null) return original
        return appendBlock(original, block, newline)
    }

    fun updateSelected(
        original: String,
        video: Video,
        enabledFields: Set<VideoUpdateField>
    ): String {
        val match = blockPattern.find(original)
        val selectedCounts = listOf(
            VideoUpdateField.WANT_COUNT,
            VideoUpdateField.WATCHED_COUNT,
            VideoUpdateField.RATING_COUNT
        ).filter { it in enabledFields }
        val includeRankings = VideoUpdateField.RANKINGS in enabledFields
        val includeReviews = VideoUpdateField.REVIEWS in enabledFields
        if (selectedCounts.isEmpty() && !includeRankings && !includeReviews) return original

        val newline = if (original.contains("\r\n")) "\r\n" else "\n"
        val existing = match?.value?.let { readExtraBlock(it) } ?: JavdbExtra()
        val merged = Video(
            number = video.number,
            wantCount = if (VideoUpdateField.WANT_COUNT in enabledFields) video.wantCount else existing.wantCount,
            watchedCount = if (VideoUpdateField.WATCHED_COUNT in enabledFields) video.watchedCount else existing.watchedCount,
            ratingCount = if (VideoUpdateField.RATING_COUNT in enabledFields) video.ratingCount else existing.ratingCount,
            rankings = if (includeRankings) VideoCollectionMerger.mergeRankings(existing.rankings, video.rankings) else existing.rankings,
            reviews = if (includeReviews) VideoCollectionMerger.mergeReviews(existing.reviews, video.reviews) else existing.reviews
        )
        val block = render(merged, newline)
            ?: return if (match != null) original.removeRange(match.range) else original
        return if (match != null) original.replaceRange(match.range, newline + block)
        else appendBlock(original, block, newline)
    }

    private fun appendBlock(original: String, block: String, newline: String): String {
        val closeStart = original.lastIndexOf("</movie>")
        if (closeStart < 0) return original
        val lineStart = original.lastIndexOf('\n', closeStart).let { if (it < 0) 0 else it + 1 }
        return original.substring(0, lineStart) + block + newline + original.substring(lineStart)
    }

    private fun readExtraBlock(block: String): JavdbExtra =
        read(javscraper.io.metadata.SecureDocumentBuilderFactory.create()
            .parse(org.xml.sax.InputSource(java.io.StringReader(block))))



    private fun render(video: Video, newline: String): String? {
        if (!video.hasJavdbExtra()) return null
        return buildString {
            append("  <javdb_extra>")
            video.wantCount?.let { appendLine("want_count", it.toString(), newline) }
            video.watchedCount?.let { appendLine("watched_count", it.toString(), newline) }
            video.ratingCount?.let { appendLine("rating_count", it.toString(), newline) }
            video.rankings.forEach { ranking ->
                append(newline)
                append("    <ranking rank=\"")
                append(ranking.rank)
                append("\">")
                append(escape(ranking.listName))
                append("</ranking>")
            }
            video.reviews.forEach { review ->
                append(newline)
                append("    <review")
                appendAttribute("id", review.id)
                appendAttribute("author", review.author)
                appendAttribute("date", review.date)
                review.score?.let { appendAttribute("score", it.toString()) }
                review.likes?.let { appendAttribute("likes", it.toString()) }
                append(">")
                append(newline)
                append("      <content>")
                append(escape(review.content))
                append("</content>")
                append(newline)
                append("    </review>")
            }
            append(newline)
            append("  </javdb_extra>")
        }
    }

    private fun StringBuilder.appendLine(name: String, value: String, newline: String) {
        append(newline)
        append("    <")
        append(name)
        append(">")
        append(escape(value))
        append("</")
        append(name)
        append(">")
    }

    private fun StringBuilder.appendAttribute(name: String, value: String) {
        if (value.isBlank()) return
        append(' ')
        append(name)
        append("=\"")
        append(escape(value))
        append('"')
    }

    private fun write(writer: XMLStreamWriter, name: String, value: String, indent: String) {
        writer.writeCharacters("\n$indent")
        writer.writeStartElement(name)
        writer.writeCharacters(value)
        writer.writeEndElement()
    }

    private fun writeRanking(writer: XMLStreamWriter, ranking: Ranking) {
        writer.writeCharacters("\n    ")
        writer.writeStartElement("ranking")
        writer.writeAttribute("rank", ranking.rank.toString())
        writer.writeCharacters(ranking.listName)
        writer.writeEndElement()
    }

    private fun writeReview(writer: XMLStreamWriter, review: Review) {
        writer.writeCharacters("\n    ")
        writer.writeStartElement("review")
        if (review.id.isNotBlank()) writer.writeAttribute("id", review.id)
        if (review.author.isNotBlank()) writer.writeAttribute("author", review.author)
        if (review.date.isNotBlank()) writer.writeAttribute("date", review.date)
        review.score?.let { writer.writeAttribute("score", it.toString()) }
        review.likes?.let { writer.writeAttribute("likes", it.toString()) }
        write(writer, "content", review.content, "      ")
        writer.writeCharacters("\n    ")
        writer.writeEndElement()
    }

    private fun readRanking(element: Element): Ranking? {
        val rank = element.getAttribute("rank").toIntOrNull() ?: return null
        val name = element.textContent.trim()
        if (name.isBlank()) return null
        return Ranking(rank = rank, listName = name)
    }

    private fun readReview(element: Element): Review? {
        val content = element.childText("content").orEmpty().trim()
        if (content.isBlank()) return null
        return Review(
            id = element.getAttribute("id"),
            author = element.getAttribute("author"),
            date = element.getAttribute("date"),
            score = element.getAttribute("score").toDoubleOrNull(),
            likes = element.getAttribute("likes").toIntOrNull(),
            content = content
        )
    }

    private fun Element.childText(name: String): String? =
        childElements(name).firstOrNull()?.textContent

    private fun Element.childElements(name: String): List<Element> =
        (0 until childNodes.length).mapNotNull { index ->
            (childNodes.item(index) as? Element)?.takeIf { it.tagName == name }
        }

    private fun Video.hasJavdbExtra(): Boolean =
        wantCount != null || watchedCount != null || ratingCount != null ||
            rankings.isNotEmpty() || reviews.isNotEmpty()

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
