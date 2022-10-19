package org.argeo.cms.acr.xml;

import javax.xml.XMLConstants;

import org.w3c.dom.Element;

public class DomUtils {
	public static void addNamespace(Element element, String prefix, String namespace) {
		element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix,
				namespace);
	}

//	public static void writeDom(TransformerFactory transformerFactory, Document document, OutputStream out)
//			throws IOException {
//		try {
//			Transformer transformer = transformerFactory.newTransformer();
//			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			DOMSource source = new DOMSource(document);
//			StreamResult result = new StreamResult(out);
//			transformer.transform(source, result);
//		} catch (TransformerException e) {
//			throw new IOException("Cannot write dom", e);
//		}
//	}

	/** singleton */
	private DomUtils() {

	}
}
