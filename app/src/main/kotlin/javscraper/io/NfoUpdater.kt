package javscraper.io

import javscraper.models.Video
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Updates existing NFO fields with targeted text patches instead of re-serializing the document. */
object NfoUpdater {
    fun update(path: Path, video: Video, lockData: Boolean): Boolean {
        val original = Files.readString(path)
        newDocumentBuilder().parse(InputSource(StringReader(original)))
        val patcher = TargetedNfoPatcher(original)
        writeFields(patcher, video, lockData)
        if (!patcher.changed) return false

        Files.writeString(path, patcher.apply())
        return true
    }

    private fun writeFields(patcher: TargetedNfoPatcher, video: Video, lockData: Boolean) {
        val title = video.title.ifBlank { video.number }
        patcher.set("title", title)
        patcher.set("originaltitle", video.number)
        patcher.set("sorttitle", video.number)
        patcher.set("num", video.number)
        patcher.setOrRemove("set", video.series)
        if (video.date.length >= 4) {
            patcher.set("year", video.date.take(4))
            patcher.set("premiered", video.date)
            patcher.set("release", video.date)
        } else {
            patcher.remove("year")
            patcher.remove("premiered")
            patcher.remove("release")
        }
        val runtime = video.duration?.takeIf { it > 0 }?.toString().orEmpty()
        patcher.setOrRemove("runtime", runtime)
        patcher.set("mpaa", "JP-18+")
        patcher.set("country", "Japan")
        patcher.set("language", "ja")
        patcher.setOrRemove("source", video.source)
        patcher.setOrRemove("website", video.detailUrl)
        val poster = video.posterUrl.ifBlank { video.coverUrl }
        patcher.setOrRemove("poster", poster)
        patcher.setOrRemove("thumb", poster)
        patcher.setOrRemove("cover", video.coverUrl)
        patcher.setOrRemove("plot", video.summary)
        patcher.setOrRemove("outline", video.summary)
        patcher.setOrRemove("studio", video.maker)
        patcher.setOrRemove("maker", video.maker)
        patcher.setOrRemove("label", video.label)
        patcher.setOrRemove("director", video.director)
        val rating = video.rating?.takeIf { it > 0 }?.toString().orEmpty()
        patcher.setOrRemove("rating", rating)
        patcher.set("lockdata", lockData.toString())

        patcher.setRepeated("genre", video.tags)
        patcher.setRepeated("tag", video.tags)
        patcher.setFanart(video.sampleImages)
        patcher.setActors(video.actresses)
        patcher.setUniqueIds(video.number)
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
}

/** Locates elements in the original text and records non-overlapping patches against it. */
private class TargetedNfoPatcher(private val original: String) {
    private val spansByPath = scanElements(original).groupBy { it.path }
    private val patches = mutableListOf<TextPatch>()
    var changed = false
        private set

    fun set(name: String, value: String) {
        val spans = spans(name)
        spans.firstOrNull()?.let { replaceText(it, value) }
        removeLines(spans.drop(1))
    }

    fun setOrRemove(name: String, value: String) {
        if (value.isBlank()) remove(name) else set(name, value)
    }

    fun remove(name: String) = removeLines(spans(name))

    fun setRepeated(name: String, values: List<String>) = synchronizeLeaf(spans(name), values)

    fun setFanart(images: List<String>) {
        val fanart = spans("fanart").firstOrNull() ?: return
        if (images.isEmpty()) {
            removeLines(listOf(fanart))
            return
        }
        synchronizeLeaf((spans("thumb", "fanart") + spans("thumb")).sortedBy { it.start }, images)
    }

    fun setActors(actresses: List<String>) {
        val actors = spans("actor")
        if (actresses.isEmpty()) {
            removeLines(actors)
            return
        }
        if (actors.isEmpty()) return

        val names = spans("name", "actor")
        val roles = spans("role", "actor")
        val orders = spans("order", "actor")
        actresses.forEachIndexed { index, name ->
            names.getOrNull(index)?.let { replaceText(it, name) }
            roles.getOrNull(index)?.let { replaceText(it, name) }
            orders.getOrNull(index)?.let { replaceText(it, (index + 1).toString()) }
        }
        if (actresses.size < actors.size) removeLines(actors.drop(actresses.size))
        if (actresses.size > actors.size) {
            val extra = actresses.drop(actors.size)
            val insertAt = lineEnd(actors.last())
            val separator = if (insertAt > 0 && original[insertAt - 1] == '\n') "" else "\n"
            val text = separator + extra.joinToString("") { cloneActor(actors.last(), it, actors.size + extra.indexOf(it) + 1) }
            patches += TextPatch(insertAt, insertAt, text)
            changed = true
        }
    }

    fun setUniqueIds(number: String) = spans("uniqueid").forEach { replaceText(it, number) }


    fun apply(): String {
        val output = StringBuilder(original)
        patches.sortedWith(compareByDescending<TextPatch> { it.start }.thenByDescending { it.end })
            .forEach { patch -> output.replace(patch.start, patch.end, patch.text) }
        return output.toString()
    }

    private fun synchronizeLeaf(spans: List<ElementSpan>, values: List<String>) {
        if (spans.isEmpty()) return
        if (spans.map { decodeXmlText(it.text) } == values) return

        val common = minOf(spans.size, values.size)
        repeat(common) { index -> replaceText(spans[index], values[index]) }
        if (values.size < spans.size) {
            removeLines(spans.drop(values.size))
        } else if (values.size > spans.size) {
            val extra = values.drop(spans.size)
            val insertAt = lineEnd(spans.last())
            val separator = if (insertAt > 0 && original[insertAt - 1] == '\n') "" else "\n"
            val text = separator + extra.joinToString("") { cloneLeafLine(spans.first(), it) }
            patches += TextPatch(insertAt, insertAt, text)
            changed = true
        }
    }

    private fun replaceText(span: ElementSpan, value: String) {
        if (decodeXmlText(span.text) == value) return
        patches += TextPatch(span.contentStart, span.contentEnd, escapeXmlText(value))
        changed = true
    }

    private fun removeLines(spans: List<ElementSpan>) {
        if (spans.isEmpty()) return
        spans.forEach { span ->
            val bounds = lineBounds(span)
            patches += TextPatch(bounds.first, bounds.second, "")
        }
        changed = true
    }

    private fun cloneLeafLine(template: ElementSpan, value: String): String {
        val bounds = lineBounds(template)
        val line = original.substring(bounds.first, bounds.second)
        val start = template.contentStart - bounds.first
        val end = template.contentEnd - bounds.first
        return line.replaceRange(start, end, escapeXmlText(value))
    }

    private fun cloneActor(template: ElementSpan, actress: String, order: Int): String {
        val bounds = lineBounds(template)
        var line = original.substring(bounds.first, bounds.second)
        val children = listOf(
            Triple("name", spans("name").getOrNull(order - 1), actress),
            Triple("role", spans("role").getOrNull(order - 1), actress),
            Triple("order", spans("order").getOrNull(order - 1), order.toString())
        )
        children.forEach { (_, span, value) ->
            if (span != null) {
                val start = span.contentStart - bounds.first
                val end = span.contentEnd - bounds.first
                if (start >= 0 && end <= line.length) line = line.replaceRange(start, end, escapeXmlText(value))
            }
        }
        return line
    }

    private fun lineBounds(span: ElementSpan): Pair<Int, Int> {
        val start = original.lastIndexOf('\n', maxOf(0, span.start - 1)) + 1
        val nextLine = original.indexOf('\n', span.end)
        val end = if (nextLine == -1) original.length else nextLine + 1
        return start to end
    }

    private fun lineEnd(span: ElementSpan): Int = lineBounds(span).second

    private fun spans(name: String, parent: String? = null): List<ElementSpan> {
        val path = listOfNotNull("movie", parent, name).joinToString(">")
        return spansByPath[path].orEmpty()
    }
}

private data class ElementSpan(
    val path: String,
    val start: Int,
    val contentStart: Int,
    val contentEnd: Int,
    val end: Int,
    val text: String
)

private data class TextPatch(val start: Int, val end: Int, val text: String)

private class OpenElement(val start: Int, val openEnd: Int, val name: String, val pathWithoutSelf: String)

private fun scanElements(original: String): List<ElementSpan> {
    val masked = Regex("""(?s)<!--.*?-->|<!\[CDATA\[.*?\]\]>""").replace(original) {
        " ".repeat(it.value.length)
    }
    val token = Regex(
        """</\s*(?<close>[^>\s/]+)\s*>|<(?<open>[^>\s/!?]+)(?<attrs>(?:"[^"]*"|'[^']*'|[^>])*)>"""
    )
    val stack = ArrayDeque<OpenElement>()
    val result = mutableListOf<ElementSpan>()
    token.findAll(masked).forEach { match ->
        val close = match.groups["close"]?.value
        val open = match.groups["open"]?.value
        when {
            close != null -> {
                val element = stack.removeLastOrNull()
                    ?: throw IllegalArgumentException("Unexpected closing element $close")
                if (element.name != close) throw IllegalArgumentException("Mismatched XML element $close")
                result += ElementSpan(
                    path = "${element.pathWithoutSelf}$close",
                    start = element.start,
                    contentStart = element.openEnd,
                    contentEnd = match.range.first,
                    end = match.range.last + 1,
                    text = original.substring(element.openEnd, match.range.first)
                )
            }
            open != null -> {
                val path = stack.joinToString(">") { it.name }
                val full = if (path.isBlank()) open else "$path>$open"
                val attrs = match.groups["attrs"]?.value.orEmpty().trimEnd()
                if (attrs.isNotEmpty() && attrs.last() == '/') {
                    val contentStart = match.range.last + 1
                    result += ElementSpan(full, match.range.first, contentStart, contentStart, match.range.last + 1, "")
                } else {
                    stack.addLast(OpenElement(match.range.first, match.range.last + 1, open,
                        if (path.isBlank()) "" else "$path>"))
                }
            }
        }
    }
    if (stack.isNotEmpty()) throw IllegalArgumentException("Unclosed XML element ${stack.last().name}")
    return result
}

private fun escapeXmlText(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun decodeXmlText(value: String): String {
    if (!value.contains('&')) return value
    return Regex("""&(?:(?<decimal>#\d+)|(?<hex>#x[0-9A-Fa-f]+)|(?<name>amp|lt|gt|quot|apos));""").replace(value) { match ->
        when {
            match.groups["decimal"] != null -> {
                val code = match.value.drop(2).dropLast(1).toIntOrNull(10)
                code?.let { String(Character.toChars(it)) } ?: match.value
            }
            match.groups["hex"] != null -> {
                val code = match.value.drop(3).dropLast(1).toIntOrNull(16)
                code?.let { String(Character.toChars(it)) } ?: match.value
            }
            else -> when (match.groups["name"]?.value) {
                "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; else -> match.value
            }
        }
    }
}
