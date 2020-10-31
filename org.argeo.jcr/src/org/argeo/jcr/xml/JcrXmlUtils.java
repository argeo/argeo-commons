package org.argeo.jcr.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.argeo.jcr.Jcr;

/** Utilities around JCR and XML. */
public class JcrXmlUtils {
	/**
	 * Convenience method calling {@link #toXmlElements(Writer, Node, boolean)} with
	 * <code>false</code>.
	 */
	public static void toXmlElements(Writer writer, Node node) throws RepositoryException, IOException {
		toXmlElements(writer, node, null, false, false, false);
	}

	/**
	 * Write JCR properties as XML elements in a tree structure whose elements are
	 * named by node primary type.
	 * 
	 * @param writer               the writer to use
	 * @param node                 the subtree
	 * @param depth                maximal depth, or if <code>null</code> the whole
	 *                             subtree. It must be positive, with depth 0
	 *                             describing just the node without its children.
	 * @param withMetadata         whether to write the primary type and mixins as
	 *                             elements
	 * @param withPrefix           whether to keep the namespace prefixes
	 * @param propertiesAsElements whether single properties should be written as
	 *                             elements rather than attributes. If
	 *                             <code>false</code>, multiple properties will be
	 *                             skipped.
	 */
	public static void toXmlElements(Writer writer, Node node, Integer depth, boolean withMetadata, boolean withPrefix,
			boolean propertiesAsElements) throws RepositoryException, IOException {
		if (depth != null && depth < 0)
			throw new IllegalArgumentException("Depth " + depth + " is negative.");

		if (node.getName().equals(Jcr.JCR_XMLTEXT)) {
			writer.write(node.getProperty(Jcr.JCR_XMLCHARACTERS).getString());
			return;
		}

		if (!propertiesAsElements) {
			Map<String, String> attrs = new TreeMap<>();
			PropertyIterator pit = node.getProperties();
			properties: while (pit.hasNext()) {
				Property p = pit.nextProperty();
				if (!p.isMultiple()) {
					String pName = p.getName();
					if (!withMetadata && (pName.equals(Jcr.JCR_PRIMARY_TYPE) || pName.equals(Jcr.JCR_UUID)
							|| pName.equals(Jcr.JCR_CREATED) || pName.equals(Jcr.JCR_CREATED_BY)
							|| pName.equals(Jcr.JCR_LAST_MODIFIED) || pName.equals(Jcr.JCR_LAST_MODIFIED_BY)))
						continue properties;
					attrs.put(withPrefix(p.getName(), withPrefix), p.getString());
				}
			}
			if (withMetadata && node.hasProperty(Property.JCR_UUID))
				attrs.put("id", "urn:uuid:" + node.getProperty(Property.JCR_UUID).getString());
			attrs.put(withPrefix ? Jcr.JCR_NAME : "name", node.getName());
			writeStart(writer, withPrefix(node.getPrimaryNodeType().getName(), withPrefix), attrs, node.hasNodes());
		} else {
			if (withMetadata && node.hasProperty(Property.JCR_UUID)) {
				writeStart(writer, withPrefix(node.getPrimaryNodeType().getName(), withPrefix), "id",
						"urn:uuid:" + node.getProperty(Property.JCR_UUID).getString());
			} else {
				writeStart(writer, withPrefix(node.getPrimaryNodeType().getName(), withPrefix));
			}
			// name
			writeStart(writer, withPrefix ? Jcr.JCR_NAME : "name");
			writer.append(node.getName());
			writeEnd(writer, withPrefix ? Jcr.JCR_NAME : "name");
		}

		// mixins
		if (withMetadata) {
			for (NodeType mixin : node.getMixinNodeTypes()) {
				writeStart(writer, withPrefix ? Jcr.JCR_MIXIN_TYPES : "mixinTypes");
				writer.append(mixin.getName());
				writeEnd(writer, withPrefix ? Jcr.JCR_MIXIN_TYPES : "mixinTypes");
			}
		}

		// properties as elements
		if (propertiesAsElements) {
			PropertyIterator pit = node.getProperties();
			properties: while (pit.hasNext()) {
				Property p = pit.nextProperty();
				if (p.isMultiple()) {
					for (Value value : p.getValues()) {
						writeStart(writer, withPrefix(p.getName(), withPrefix));
						writer.write(value.getString());
						writeEnd(writer, withPrefix(p.getName(), withPrefix));
					}
				} else {
					Value value = p.getValue();
					String pName = p.getName();
					if (!withMetadata && (pName.equals(Jcr.JCR_PRIMARY_TYPE) || pName.equals(Jcr.JCR_UUID)
							|| pName.equals(Jcr.JCR_CREATED) || pName.equals(Jcr.JCR_CREATED_BY)
							|| pName.equals(Jcr.JCR_LAST_MODIFIED) || pName.equals(Jcr.JCR_LAST_MODIFIED_BY)))
						continue properties;
					writeStart(writer, withPrefix(p.getName(), withPrefix));
					writer.write(value.getString());
					writeEnd(writer, withPrefix(p.getName(), withPrefix));
				}
			}
		}

		// children
		if (node.hasNodes()) {
			if (depth == null || depth > 0) {
				NodeIterator nit = node.getNodes();
				while (nit.hasNext()) {
					toXmlElements(writer, nit.nextNode(), depth == null ? null : depth - 1, withMetadata, withPrefix,
							propertiesAsElements);
				}
			}
			writeEnd(writer, withPrefix(node.getPrimaryNodeType().getName(), withPrefix));
		}
	}

	private static String withPrefix(String str, boolean withPrefix) {
		if (withPrefix)
			return str;
		int index = str.indexOf(':');
		if (index < 0)
			return str;
		return str.substring(index + 1);
	}

	private static void writeStart(Writer writer, String tagName) throws IOException {
		writer.append('<');
		writer.append(tagName);
		writer.append('>');
	}

	private static void writeStart(Writer writer, String tagName, String attr, String value) throws IOException {
		writer.append('<');
		writer.append(tagName);
		writer.append(' ');
		writer.append(attr);
		writer.append("=\"");
		writer.append(value);
		writer.append("\">");
	}

	private static void writeStart(Writer writer, String tagName, Map<String, String> attrs, boolean hasChildren)
			throws IOException {
		writer.append('<');
		writer.append(tagName);
		for (String attr : attrs.keySet()) {
			writer.append(' ');
			writer.append(attr);
			writer.append("=\"");
			writer.append(attrs.get(attr));
			writer.append('\"');
		}
		if (hasChildren)
			writer.append('>');
		else
			writer.append("/>");
	}

	private static void writeEnd(Writer writer, String tagName) throws IOException {
		writer.append("</");
		writer.append(tagName);
		writer.append('>');
	}

	/** Singleton. */
	private JcrXmlUtils() {

	}

}
