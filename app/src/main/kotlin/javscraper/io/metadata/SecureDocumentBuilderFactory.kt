package javscraper.io.metadata

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

internal object SecureDocumentBuilderFactory {
    fun create() =
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
