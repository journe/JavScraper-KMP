package javscraper.io

import javscraper.models.Video
import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

object NfoWriter {
    private val factory = XMLOutputFactory.newInstance()

    fun generate(video: Video): String {
        val sw = StringWriter()
        val w = factory.createXMLStreamWriter(sw)
        w.writeStartDocument("UTF-8", "1.0")
        w.writeProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?")
        w.writeStartElement("movie")
        write(w, "title", video.title.ifBlank { video.number })
        write(w, "originaltitle", video.number)
        write(w, "sorttitle", video.number)
        if (video.series.isNotBlank()) write(w, "set", video.series)
        if (video.date.length >= 4) { write(w, "year", video.date.take(4)); write(w, "premiered", video.date); write(w, "release", video.date) }
        if (video.duration != null && video.duration > 0) write(w, "runtime", video.duration.toString())
        write(w, "mpaa", "JP-18+")
        if (video.summary.isNotBlank()) { write(w, "plot", video.summary); write(w, "outline", video.summary) }
        if (video.maker.isNotBlank()) { write(w, "studio", video.maker); write(w, "maker", video.maker) }
        if (video.label.isNotBlank()) write(w, "label", video.label)
        if (video.director.isNotBlank()) write(w, "director", video.director)
        video.actresses.forEachIndexed { i, n -> w.writeStartElement("actor"); write(w, "name", n); write(w, "role", n); write(w, "order", (i+1).toString()); w.writeEndElement() }
        video.tags.forEach { write(w, "genre", it); write(w, "tag", it) }
        if (video.rating != null && video.rating > 0) write(w, "rating", video.rating!!.toString())
        w.writeEndElement(); w.writeEndDocument(); w.flush()
        return sw.toString()
    }
    private fun write(w: XMLStreamWriter, n: String, v: String) { w.writeStartElement(n); w.writeCharacters(v); w.writeEndElement() }
}