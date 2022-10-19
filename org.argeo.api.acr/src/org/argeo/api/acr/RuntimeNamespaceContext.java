package org.argeo.api.acr;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Programmatically defined {@link NamespaceContext}, code contributing
 * namespaces MUST register here with a single default prefix.
 */
public class RuntimeNamespaceContext implements NamespaceContext {
	public final static String XSD_DEFAULT_PREFIX = "xs";
	public final static String XSD_INSTANCE_DEFAULT_PREFIX = "xsi";

	private NavigableMap<String, String> prefixes = new TreeMap<>();
	private NavigableMap<String, String> namespaces = new TreeMap<>();

	@Override
	public String getPrefix(String namespaceURI) {
		return NamespaceUtils.getPrefix((ns) -> {
			String prefix = namespaces.get(ns);
			if (prefix == null)
				throw new IllegalStateException("Namespace " + ns + " is not registered.");
			return prefix;
		}, namespaceURI);
	}

	@Override
	public String getNamespaceURI(String prefix) {
		return NamespaceUtils.getNamespaceURI((p) -> {
			String ns = prefixes.get(p);
			if (ns == null)
				throw new IllegalStateException("Prefix " + p + " is not registered.");
			return ns;
		}, prefix);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return Collections.singleton(getPrefix(namespaceURI)).iterator();
	}

	/*
	 * STATIC
	 */

	private final static RuntimeNamespaceContext INSTANCE = new RuntimeNamespaceContext();

	static {
		// Standard
		register(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
		register(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);

		// Common
		register(XMLConstants.W3C_XML_SCHEMA_NS_URI, XSD_DEFAULT_PREFIX);
		register(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, XSD_INSTANCE_DEFAULT_PREFIX);

		// Argeo specific
		register(CrName.CR_NAMESPACE_URI, CrName.CR_DEFAULT_PREFIX);
		register(CrName.LDAP_NAMESPACE_URI, CrName.LDAP_DEFAULT_PREFIX);
		register(CrName.ROLE_NAMESPACE_URI, CrName.ROLE_DEFAULT_PREFIX);
	}

	public static NamespaceContext getNamespaceContext() {
		return INSTANCE;
	}

	public static Map<String, String> getPrefixes() {
		return Collections.unmodifiableNavigableMap(INSTANCE.prefixes);
	}

	public synchronized static void register(String namespaceURI, String prefix) {
		NavigableMap<String, String> prefixes = INSTANCE.prefixes;
		NavigableMap<String, String> namespaces = INSTANCE.namespaces;
		if (prefixes.containsKey(prefix)) {
			String ns = prefixes.get(prefix);
			if (ns.equals(namespaceURI))
				return; // ignore silently
			throw new IllegalStateException("Prefix " + prefix + " is already registered with namespace URI " + ns);
		}
		if (namespaces.containsKey(namespaceURI)) {
			String p = namespaces.get(namespaceURI);
			if (p.equals(prefix))
				return; // ignore silently
			throw new IllegalStateException("Namespace " + namespaceURI + " is already registered with prefix " + p);
		}
		prefixes.put(prefix, namespaceURI);
		namespaces.put(namespaceURI, prefix);
	}
}
