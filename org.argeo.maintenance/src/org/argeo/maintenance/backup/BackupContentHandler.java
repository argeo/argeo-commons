package org.argeo.maintenance.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Base64;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.argeo.jcr.JcrException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** XML handler serialising a JCR system view. */
public class BackupContentHandler extends DefaultHandler {
	final static int MAX_DEPTH = 1024;
	final static String SV_NAMESPACE_URI = "http://www.jcp.org/jcr/sv/1.0";
	final static String SV_PREFIX = "sv";
	// elements
	final static String NODE = "node";
	final static String PROPERTY = "property";
	final static String VALUE = "value";
	// attributes
	final static String NAME = "name";
	final static String MULTIPLE = "multiple";
	final static String TYPE = "type";

	// values
	final static String BINARY = "Binary";
	final static String JCR_CONTENT = "jcr:content";

	private Writer out;
	private Session session;
	private Set<String> contentPaths = new TreeSet<>();

//	private boolean inSystem = false;

	public BackupContentHandler(Writer out, Session session) {
		super();
		this.out = out;
		this.session = session;
	}

	private int currentDepth = -1;
	private String[] currentPath = new String[MAX_DEPTH];

	private boolean currentPropertyIsMultiple = false;
	private String currentEncoded = null;
	private Base64.Encoder base64encore = Base64.getEncoder();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		boolean isNode;
		boolean isProperty;
		switch (localName) {
		case NODE:
			isNode = true;
			isProperty = false;
			break;
		case PROPERTY:
			isNode = false;
			isProperty = true;
			break;
		default:
			isNode = false;
			isProperty = false;
		}

		if (isNode) {
			String nodeName = attributes.getValue(SV_NAMESPACE_URI, NAME);
			currentDepth = currentDepth + 1;
//			if (currentDepth >= 0)
			currentPath[currentDepth] = nodeName;
//			System.out.println(getCurrentPath() + " , depth=" + currentDepth);
//			if ("jcr:system".equals(nodeName)) {
//				inSystem = true;
//			}
		}
//		if (inSystem)
//			return;

		if (SV_NAMESPACE_URI.equals(uri))
			try {
				out.write("<");
				out.write(SV_PREFIX + ":" + localName);
				if (isProperty)
					currentPropertyIsMultiple = false; // always reset
				for (int i = 0; i < attributes.getLength(); i++) {
					String ns = attributes.getURI(i);
					if (SV_NAMESPACE_URI.equals(ns)) {
						String attrName = attributes.getLocalName(i);
						String attrValue = attributes.getValue(i);
						out.write(" ");
						out.write(SV_PREFIX + ":" + attrName);
						out.write("=");
						out.write("\"");
						out.write(attrValue);
						out.write("\"");
						if (isProperty) {
							if (MULTIPLE.equals(attrName))
								currentPropertyIsMultiple = Boolean.parseBoolean(attrValue);
							else if (TYPE.equals(attrName)) {
								if (BINARY.equals(attrValue)) {
									if (JCR_CONTENT.equals(getCurrentName())) {
										contentPaths.add(getCurrentPath());
									} else {
										Binary binary = session.getNode(getCurrentPath()).getProperty(attrName)
												.getBinary();
										try (InputStream in = binary.getStream()) {
											currentEncoded = base64encore.encodeToString(IOUtils.toByteArray(in));
										} finally {

										}
									}
								}
							}
						}
					}
				}
				if (isNode && currentDepth == 0) {
					// out.write(" xmlns=\"" + SV_NAMESPACE_URI + "\"");
					out.write(" xmlns:" + SV_PREFIX + "=\"" + SV_NAMESPACE_URI + "\"");
				}
				out.write(">");
				if (isNode)
					out.write("\n");
				else if (isProperty && currentPropertyIsMultiple)
					out.write("\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (RepositoryException e) {
				throw new JcrException(e);
			}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equals(NODE)) {
//			System.out.println("endElement " + getCurrentPath() + " , depth=" + currentDepth);
//			if (currentDepth > 0)
			currentPath[currentDepth] = null;
			currentDepth = currentDepth - 1;
			assert currentDepth >= 0;
//			if (inSystem) {
//				// System.out.println("Skip " + getCurrentPath()+" ,
//				// currentDepth="+currentDepth);
//				if (currentDepth == 0) {
//					inSystem = false;
//					return;
//				}
//			}
		}
//		if (inSystem)
//			return;
		boolean isValue = localName.equals(VALUE);
		if (SV_NAMESPACE_URI.equals(uri))
			try {
				if (isValue && currentEncoded != null) {
					out.write(currentEncoded);
				}
				currentEncoded = null;
				out.write("</");
				out.write(SV_PREFIX + ":" + localName);
				out.write(">");
				if (!isValue)
					out.write("\n");
				else {
					if (currentPropertyIsMultiple)
						out.write("\n");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
//		if (inSystem)
//			return;
		try {
			out.write(ch, start, length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getCurrentName() {
		assert currentDepth >= 0;
//		if (currentDepth == 0)
//			return "jcr:root";
		return currentPath[currentDepth];
	}

	protected String getCurrentPath() {
//		if (currentDepth == 0)
//			return "/";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= currentDepth; i++) {
//			if (i != 0)
				sb.append('/');
			sb.append(currentPath[i]);
		}
		return sb.toString();
	}

	public Set<String> getContentPaths() {
		return contentPaths;
	}

}
