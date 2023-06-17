package org.argeo.api.acr;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Programmatically defined {@link NamespaceContext}, which is valid at runtime
 * (when the software is running). Code contributing namespaces MUST register
 * here with a single default prefix, nad MUST make sure that stored data
 * contains the fully qualified namespace URI.
 */
public class RuntimeNamespaceContext implements NamespaceContext {
	public final static String XSD_DEFAULT_PREFIX = "xs";
	public final static String XSD_INSTANCE_DEFAULT_PREFIX = "xsi";

	private NavigableMap<String, String> prefixes = new TreeMap<>();
	private NavigableMap<String, String> namespaces = new TreeMap<>();

	/*
	 * NAMESPACE CONTEXT IMPLEMENTATION
	 */

	@Override
	public String getPrefix(String namespaceURI) throws IllegalArgumentException {
		return NamespaceUtils.getPrefix((ns) -> {
			String prefix = namespaces.get(ns);
			if (prefix == null)
				throw new IllegalArgumentException("Namespace " + ns + " is not registered.");
			return prefix;
		}, namespaceURI);
	}

	@Override
	public String getNamespaceURI(String prefix) throws IllegalArgumentException {
		return NamespaceUtils.getNamespaceURI((p) -> {
			String ns = prefixes.get(p);
			if (ns == null)
				throw new IllegalArgumentException("Prefix " + p + " is not registered.");
			return ns;
		}, prefix);
	}

	@Override
	public Iterator<String> getPrefixes(String namespaceURI) throws IllegalArgumentException {
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
		register(ArgeoNamespace.CR_NAMESPACE_URI, ArgeoNamespace.CR_DEFAULT_PREFIX);
		register(ArgeoNamespace.LDAP_NAMESPACE_URI, ArgeoNamespace.LDAP_DEFAULT_PREFIX);
		register(ArgeoNamespace.ROLE_NAMESPACE_URI, ArgeoNamespace.ROLE_DEFAULT_PREFIX);
	}

	/** The runtime namespace context instance. */
	public static NamespaceContext getNamespaceContext() {
		return INSTANCE;
	}

	/** The registered prefixes. */
	public static Map<String, String> getPrefixes() {
		return Collections.unmodifiableNavigableMap(INSTANCE.prefixes);
	}

	/** Registers a namespace URI / default prefix mapping. */
	public synchronized static void register(String namespaceURI, String defaultPrefix) {
		NavigableMap<String, String> prefixes = INSTANCE.prefixes;
		NavigableMap<String, String> namespaces = INSTANCE.namespaces;
		if (prefixes.containsKey(defaultPrefix)) {
			String ns = prefixes.get(defaultPrefix);
			if (ns.equals(namespaceURI))
				return; // ignore silently
			throw new IllegalStateException(
					"Prefix " + defaultPrefix + " is already registered with namespace URI " + ns);
		}
		if (namespaces.containsKey(namespaceURI)) {
			String p = namespaces.get(namespaceURI);
			if (p.equals(defaultPrefix))
				return; // ignore silently
			throw new IllegalStateException("Namespace " + namespaceURI + " is already registered with prefix " + p);
		}
		prefixes.put(defaultPrefix, namespaceURI);
		namespaces.put(namespaceURI, defaultPrefix);
	}
}
