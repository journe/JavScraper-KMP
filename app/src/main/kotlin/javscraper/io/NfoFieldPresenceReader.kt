package javscraper.io

import javscraper.models.VideoUpdateField
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilder

object NfoFieldPresenceReader {
    fun read(path: Path): Set<VideoUpdateField> {
        val document = newBuilder().parse(InputSource(StringReader(Files.readString(path).removePrefix("\uFEFF"))))
        val present = mutableSetOf<VideoUpdateField>()
        if (document.has("num", "originaltitle", "sorttitle", "uniqueid", "id")) present += VideoUpdateField.NUMBER
        if (document.has("title")) present += VideoUpdateField.TITLE
        if (document.has("year", "premiered", "release", "releasedate")) present += VideoUpdateField.DATE
        if (document.hasActorName()) present += VideoUpdateField.ACTRESSES
        if (document.has("plot", "outline")) present += VideoUpdateField.SUMMARY
        if (document.has("studio", "maker")) present += VideoUpdateField.MAKER
        if (document.has("label", "publisher")) present += VideoUpdateField.LABEL
        if (document.has("series") || document.hasSetName()) present += VideoUpdateField.SERIES
        if (document.has("director")) present += VideoUpdateField.DIRECTOR
        if (document.has("runtime")) present += VideoUpdateField.DURATION
        if (document.has("rating")) present += VideoUpdateField.RATING
        if (document.hasExtra("want_count")) present += VideoUpdateField.WANT_COUNT
        if (document.hasExtra("watched_count")) present += VideoUpdateField.WATCHED_COUNT
        if (document.hasExtra("rating_count")) present += VideoUpdateField.RATING_COUNT
        if (document.has("genre", "tag")) present += VideoUpdateField.TAGS
        if (document.hasExtraRanking()) present += VideoUpdateField.RANKINGS
        if (document.hasExtraReview()) present += VideoUpdateField.REVIEWS
        if (document.has("source")) present += VideoUpdateField.SOURCE
        if (document.has("website", "detailurl")) present += VideoUpdateField.DETAIL_URL
        return present
    }

    private fun newBuilder(): DocumentBuilder =
        javscraper.io.metadata.SecureDocumentBuilderFactory.create()

    private fun Document.has(vararg names: String): Boolean = names.any { name ->
        getElementsByTagName(name).length > 0
    }

    private fun Document.hasActorName(): Boolean =
        childElements("actor").any { actor -> actor.childElements("name").isNotEmpty() }

    private fun Document.hasSetName(): Boolean =
        childElements("set").any { set -> set.childElements("name").isNotEmpty() }

    private fun Document.extraRoot(): Element? =
        getElementsByTagName("javdb_extra").item(0) as? Element

    private fun Document.hasExtra(name: String): Boolean =
        extraRoot()?.childElements(name)?.isNotEmpty() == true

    private fun Document.hasExtraRanking(): Boolean =
        extraRoot()?.childElements("ranking")?.isNotEmpty() == true

    private fun Document.hasExtraReview(): Boolean =
        extraRoot()?.childElements("review")?.isNotEmpty() == true

    private fun Document.childElements(name: String): List<Element> {
        val nodes = getElementsByTagName(name)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun Element.childElements(name: String): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
            .filter { it.tagName == name }
}