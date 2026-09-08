package javscraper.io

import javscraper.models.Video
import mu.KotlinLogging
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object NfoReader {
    private val log = KotlinLogging.logger {}
    private val bareAmpersand = Regex("&(?!amp;|lt;|gt;|quot;|apos;|#)")
    private val xmlDeclaration = Regex("<\\?xml[^>]*\\?+>")

    fun read(path: Path): Video? {
        if (!Files.isRegularFile(path)) return null
        return try {
            val content = keepFirstXmlDeclaration(Files.readString(path))
            val safeContent = bareAmpersand.replace(content, "&amp;")
            val document = newDocumentBuilder().parse(InputSource(StringReader(safeContent)))
            Video(
                number = readNumber(document),
                title = readText(document, "title").ifBlank { readNumber(document) },
                actresses = readActorNames(document),
                date = normalizeDate(
                    readText(document, "release", "releasedate", "premiered", "year")
                ),
                maker = readText(document, "studio", "maker"),
                label = readText(document, "label", "publisher"),
                series = readSetNames(document).firstOrNull().orEmpty()
                    .ifBlank { readText(document, "series", "set") },
                director = readText(document, "director"),
                duration = readText(document, "runtime").trim().toIntOrNull(),
                rating = readText(document, "rating").trim().toDoubleOrNull(),
                tags = readTags(document),
                source = readText(document, "source"),
                detailUrl = readText(document, "website", "detailurl"),
                posterUrl = readText(document, "thumb", "cover", "poster"),
                summary = readText(document, "plot", "outline")
            )
        } catch (e: Exception) {
            log.warn(e) { "NFO read failed: $path" }
            null
        }
    }

    private fun newDocumentBuilder() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }.newDocumentBuilder()

    private fun readNumber(document: Document): String {
        val explicit = readText(document, "num", "id").trim()
        if (explicit.isNotBlank()) return explicit
        return readText(document, "originaltitle", "sorttitle").trim()
    }

    private fun readTags(document: Document): List<String> {
        val values = readAllText(document, "genre") + readAllText(document, "tag")
        return values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    private fun readText(document: Document, vararg paths: String): String =
        paths.firstNotNullOfOrNull { path ->
            document.getElementsByTagName(path).takeIf { it.length > 0 }?.item(0)?.textContent
        }.orEmpty()

    private fun readAllText(document: Document, path: String): List<String> {
        val nodes = document.getElementsByTagName(path)
        return List(nodes.length) { index -> nodes.item(index).textContent }
    }

    private fun readActorNames(document: Document): List<String> =
        childTexts(document, "actor", "name")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun readSetNames(document: Document): List<String> =
        childTexts(document, "set", "name")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun childTexts(document: Document, parentTag: String, childTag: String): List<String> {
        val parents = document.getElementsByTagName(parentTag)
        return List(parents.length) { index -> parents.item(index) }
            .mapNotNull { parent -> (parent as? Element)?.childText(childTag) }
    }

    private fun Element.childText(name: String): String? {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.tagName == name) return child.textContent
        }
        return null
    }

    private fun keepFirstXmlDeclaration(content: String): String {
        var seen = false
        return xmlDeclaration.replace(content) { match ->
            if (seen) {
                ""
            } else {
                seen = true
                match.value
            }
        }
    }
    private fun normalizeDate(value: String): String {
        val match = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(value) ?: return value.trim()
        val (year, month, day) = match.destructured
        return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }
}