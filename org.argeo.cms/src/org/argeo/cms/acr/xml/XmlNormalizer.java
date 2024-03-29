package org.argeo.cms.acr.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Consistently normalises an XML in order to ease diff (typically in a
 * versioning system).
 */
public class XmlNormalizer {
//	private final static Logger logger = System.getLogger(XmlNormalizer.class.getName());

	private DocumentBuilder documentBuilder;
	private TransformerFactory transformerFactory;

	private DOMSource stripSpaceXsl;

	public XmlNormalizer() {
		this(2);
	}

	public XmlNormalizer(int indent) {
		try {
			documentBuilder = DocumentBuilderFactory.newNSInstance().newDocumentBuilder();
			transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			try (InputStream in = XmlNormalizer.class.getResourceAsStream("stripSpaces.xsl")) {
				DOMResult result = new DOMResult();
				transformerFactory.newTransformer().transform(new StreamSource(in), result);
				stripSpaceXsl = new DOMSource(result.getNode());
			}
		} catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException
				| IOException e) {
			throw new IllegalStateException("Cannot initialise document builder and transformer", e);
		}
	}

	public void normalizeXmlFiles(DirectoryStream<Path> ds) throws IOException {
		for (Path path : ds) {
			normalizeXmlFile(path);
		}
	}

	public void normalizeXmlFile(Path path) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
				OutputStream out = Files.newOutputStream(path)) {
			normalizeAndIndent(in, out);
//			logger.log(Level.DEBUG, () -> "Normalized XML " + path);
		}
	}

	public void normalizeAndIndent(Source source, Result result) {
		try {
			Transformer transformer = transformerFactory.newTransformer(stripSpaceXsl);
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new RuntimeException("Cannot strip space from " + source, e);
		}
	}

	public void normalizeAndIndent(InputStream in, OutputStream out) throws IOException {
		try {
			Document document = documentBuilder.parse(in);

			// clear whitespaces outside tags
			document.normalize();
//			XPath xPath = XPathFactory.newInstance().newXPath();
//			NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", document,
//					XPathConstants.NODESET);
//
//			for (int i = 0; i < nodeList.getLength(); ++i) {
//				Node node = nodeList.item(i);
//				node.getParentNode().removeChild(node);
//			}

			normalizeAndIndent(new DOMSource(document), new StreamResult(out));
		} catch (DOMException | IllegalArgumentException | SAXException | TransformerFactoryConfigurationError e) {
			throw new RuntimeException("Cannot normalise and indent XML", e);
		}
	}

	public static void print(Source source, int indent) {
		XmlNormalizer xmlNormalizer = new XmlNormalizer(indent);
		xmlNormalizer.normalizeAndIndent(source, new StreamResult(System.out));
	}

	public static void print(Source source) {
		print(source, 2);
	}

	public static void main(String[] args) throws IOException {
		Path dir = Paths.get(args[0]);
		XmlNormalizer xmlNormalizer = new XmlNormalizer();
		DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.svg");
		xmlNormalizer.normalizeXmlFiles(ds);

	}
}
