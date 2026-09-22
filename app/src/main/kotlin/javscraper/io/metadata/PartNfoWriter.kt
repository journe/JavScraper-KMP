package javscraper.io.metadata

import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

/** Copies the master movie NFO and replaces only the part title. */
internal object PartNfoWriter {
    private val xpath = XPathFactory.newInstance().newXPath()

    fun write(masterNfo: Path, target: Path, title: String) {
        val raw = Files.readString(masterNfo).removePrefix("\uFEFF")
        val document = parse(raw)
        val nodes = xpath.evaluate("/movie/title", document, XPathConstants.NODESET) as NodeList
        val count = nodes.length
        require(count == 1) { "movie.nfo must contain exactly one title: $masterNfo" }
        nodes.item(0).textContent = title

        Files.createDirectories(target.parent)
        Files.writeString(target, serialize(document))
    }

    private fun parse(xml: String): Document =
        SecureDocumentBuilderFactory.create().parse(InputSource(StringReader(xml)))

    private fun serialize(document: Document): String {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        val writer = java.io.StringWriter()
        transformer.transform(javax.xml.transform.dom.DOMSource(document), javax.xml.transform.stream.StreamResult(writer))
        return writer.toString()
    }
}
