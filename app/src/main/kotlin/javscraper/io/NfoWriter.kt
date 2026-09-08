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
        w.writeCharacters("\n")
        w.writeStartElement("movie")
        write(w, "title", video.title.ifBlank { video.number })
        write(w, "originaltitle", video.number)
        write(w, "sorttitle", video.number)
        if (video.series.isNotBlank()) write(w, "set", video.series)
        if (video.date.length >= 4) {
            write(w, "year", video.date.take(4))
            write(w, "premiered", video.date)
            write(w, "release", video.date)
        }
        if (video.duration != null && video.duration > 0) write(w, "runtime", video.duration.toString())
        write(w, "mpaa", "JP-18+")
        if (video.source.isNotBlank()) write(w, "source", video.source)
        if (video.detailUrl.isNotBlank()) write(w, "website", video.detailUrl)
        val poster = video.posterUrl.ifBlank { video.coverUrl }
        if (poster.isNotBlank()) write(w, "thumb", poster)
        if (video.coverUrl.isNotBlank()) write(w, "cover", video.coverUrl)
        if (video.sampleImages.isNotEmpty()) writeFanart(w, video.sampleImages)
        if (video.summary.isNotBlank()) {
            write(w, "plot", video.summary)
            write(w, "outline", video.summary)
        }
        if (video.maker.isNotBlank()) {
            write(w, "studio", video.maker)
            write(w, "maker", video.maker)
        }
        if (video.label.isNotBlank()) write(w, "label", video.label)
        if (video.director.isNotBlank()) write(w, "director", video.director)
        video.actresses.forEachIndexed { index, name -> writeActor(w, name, index + 1) }
        video.tags.forEach {
            write(w, "genre", it)
            write(w, "tag", it)
        }
        if (video.rating != null && video.rating > 0) write(w, "rating", video.rating.toString())
        w.writeCharacters("\n")
        w.writeEndElement()
        w.writeEndDocument()
        w.flush()
        return sw.toString()
    }

    private fun write(w: XMLStreamWriter, name: String, value: String, indent: String = "  ") {
        w.writeCharacters("\n$indent")
        w.writeStartElement(name)
        w.writeCharacters(value)
        w.writeEndElement()
    }

    private fun writeFanart(w: XMLStreamWriter, images: List<String>) {
        w.writeCharacters("\n  ")
        w.writeStartElement("fanart")
        images.forEach { write(w, "thumb", it, "    ") }
        w.writeCharacters("\n  ")
        w.writeEndElement()
    }

    private fun writeActor(w: XMLStreamWriter, name: String, order: Int) {
        w.writeCharacters("\n  ")
        w.writeStartElement("actor")
        write(w, "name", name, "    ")
        write(w, "role", name, "    ")
        write(w, "order", order.toString(), "    ")
        w.writeCharacters("\n  ")
        w.writeEndElement()
    }
}
