package org.argeo.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/** Uilities around the JCR extensions. */
public class JcrxApi {
	public final static String MD5 = "MD5";
	public final static String SHA1 = "SHA1";
	public final static String SHA256 = "SHA-256";
	public final static String SHA512 = "SHA-512";

	public final static String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";
	public final static String EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
	public final static String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
	public final static String EMPTY_SHA512 = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";

	public final static int LENGTH_MD5 = EMPTY_MD5.length();
	public final static int LENGTH_SHA1 = EMPTY_SHA1.length();
	public final static int LENGTH_SHA256 = EMPTY_SHA256.length();
	public final static int LENGTH_SHA512 = EMPTY_SHA512.length();

	/*
	 * XML
	 */
	/**
	 * Set as a subnode which will be exported as an XML element.
	 */
	public static String getXmlValue(Node node, String name) {
		try {
			if (!node.hasNode(name))
				return null;
			Node child = node.getNode(name);
			if (child.hasNode(Jcr.JCR_XMLTEXT))
				return null;
			Node xmlText = child.getNode(Jcr.JCR_XMLTEXT);
			if (!xmlText.hasProperty(Jcr.JCR_XMLCHARACTERS))
				throw new IllegalArgumentException(
						"Node " + xmlText + " has no " + Jcr.JCR_XMLCHARACTERS + " property");
			return xmlText.getProperty(Jcr.JCR_XMLCHARACTERS).getString();
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

	/**
	 * Add a checksum replacing the one which was previously set with the same
	 * length.
	 */
	public static void addChecksum(Node node, String checksum) {
		try {
			if (!node.hasProperty(JcrxName.JCRX_SUM)) {
				node.setProperty(JcrxName.JCRX_SUM, new String[] { checksum });
				return;
			} else {
				int stringLength = checksum.length();
				Property property = node.getProperty(JcrxName.JCRX_SUM);
				List<Value> values = Arrays.asList(property.getValues());
				Integer indexToRemove = null;
				values: for (int i = 0; i < values.size(); i++) {
					Value value = values.get(i);
					if (value.getString().length() == stringLength) {
						indexToRemove = i;
						break values;
					}
				}
				if (indexToRemove != null)
					values.set(indexToRemove, node.getSession().getValueFactory().createValue(checksum));
				else
					values.add(0, node.getSession().getValueFactory().createValue(checksum));
				property.setValue(values.toArray(new Value[values.size()]));
			}
		} catch (RepositoryException e) {
			throw new JcrException("Cannot set checksum on " + node, e);
		}
	}

	/** Replace all checksums. */
	public static void setChecksums(Node node, List<String> checksums) {
		try {
			node.setProperty(JcrxName.JCRX_SUM, checksums.toArray(new String[checksums.size()]));
		} catch (RepositoryException e) {
			throw new JcrException("Cannot set checksums on " + node, e);
		}
	}

	/** Replace all checksums. */
	public static List<String> getChecksums(Node node) {
		try {
			List<String> res = new ArrayList<>();
			if (!node.hasProperty(JcrxName.JCRX_SUM))
				return res;
			Property property = node.getProperty(JcrxName.JCRX_SUM);
			for (Value value : property.getValues()) {
				res.add(value.getString());
			}
			return res;
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get checksums from " + node, e);
		}
	}

//	/** Replace all checksums with this single one. */
//	public static void setChecksum(Node node, String checksum) {
//		setChecksums(node, Collections.singletonList(checksum));
//	}

	/** Retrieves the checksum with this algorithm, or null if not found. */
	public static String getChecksum(Node node, String algorithm) {
		int stringLength;
		switch (algorithm) {
		case MD5:
			stringLength = LENGTH_MD5;
			break;
		case SHA1:
			stringLength = LENGTH_SHA1;
			break;
		case SHA256:
			stringLength = LENGTH_SHA256;
			break;
		case SHA512:
			stringLength = LENGTH_SHA512;
			break;
		default:
			throw new IllegalArgumentException("Unkown algorithm " + algorithm);
		}
		return getChecksum(node, stringLength);
	}

	/** Retrieves the checksum with this string length, or null if not found. */
	public static String getChecksum(Node node, int stringLength) {
		try {
			if (!node.hasProperty(JcrxName.JCRX_SUM))
				return null;
			Property property = node.getProperty(JcrxName.JCRX_SUM);
			for (Value value : property.getValues()) {
				String str = value.getString();
				if (str.length() == stringLength)
					return str;
			}
			return null;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get checksum for " + node, e);
		}
	}

}
