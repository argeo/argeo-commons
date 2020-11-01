package org.argeo.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/** Uilities around the JCR extensions. */
public class JcrxUtils {

	/*
	 * XML
	 */
	/**
	 * Set as a subnode which will be exported as an XML element.
	 */
	public static String getXmlValue(Node node, String name) {
		try {
			if (!node.hasNode(name))
				throw new IllegalArgumentException("No XML text named " + name);
			return node.getNode(name).getNode(Jcr.JCR_XMLTEXT).getProperty(Jcr.JCR_XMLCHARACTERS).getString();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get " + name + " as XML text", e);
		}
	}

	/**
	 * Set as a subnode which will be exported as an XML element.
	 */
	public static void setXmlValue(Node node, String name, String value) {
		try {
			if (node.hasNode(name))
				node.getNode(name).getNode(Jcr.JCR_XMLTEXT).setProperty(Jcr.JCR_XMLCHARACTERS, value);
			else
				node.addNode(name, JcrxType.JCRX_XMLVALUE).addNode(Jcr.JCR_XMLTEXT, JcrxType.JCRX_XMLTEXT)
						.setProperty(Jcr.JCR_XMLCHARACTERS, value);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot set " + name + " as XML text", e);
		}
	}

}
