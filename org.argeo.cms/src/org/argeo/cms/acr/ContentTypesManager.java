package org.argeo.cms.acr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSException;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSTypeDefinition;
import org.argeo.api.acr.CrName;
import org.argeo.api.cms.CmsLog;
import org.xml.sax.SAXException;

public class ContentTypesManager {
	private final static CmsLog log = CmsLog.getLog(ContentTypesManager.class);
	private Map<String, String> prefixes = new TreeMap<>();

	private List<Source> sources = new ArrayList<>();

	private SchemaFactory schemaFactory;
	private Schema schema;

	public ContentTypesManager() {
		schemaFactory = SchemaFactory.newDefaultInstance();

	}

	public synchronized void init() {
//		prefixes.put(CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
		prefixes.put("basic", CrName.CR_NAMESPACE_URI);
		prefixes.put("owner", CrName.CR_NAMESPACE_URI);
		prefixes.put("posix", CrName.CR_NAMESPACE_URI);

		try {
			for (CmsContentTypes cs : CmsContentTypes.values()) {
				StreamSource source = new StreamSource(cs.getResource().toExternalForm());
				sources.add(source);
				if (prefixes.containsKey(cs.getDefaultPrefix()))
					throw new IllegalStateException("Prefix " + cs.getDefaultPrefix() + " is already mapped with "
							+ prefixes.get(cs.getDefaultPrefix()));
				prefixes.put(cs.getDefaultPrefix(), cs.getNamespace());
			}

			schema = schemaFactory.newSchema(sources.toArray(new Source[sources.size()]));
		} catch (SAXException e) {
			throw new IllegalStateException("Cannot initialise types", e);
		}

	}

	public synchronized void registerTypes(String defaultPrefix, String namespace, String xsdSystemId) {
		try {
			if (prefixes.containsKey(defaultPrefix))
				throw new IllegalStateException(
						"Prefix " + defaultPrefix + " is already mapped with " + prefixes.get(defaultPrefix));
			prefixes.put(defaultPrefix, namespace);

			sources.add(new StreamSource(xsdSystemId));
			schema = schemaFactory.newSchema(sources.toArray(new Source[sources.size()]));
		} catch (SAXException e) {
			throw new IllegalStateException("Cannot initialise types " + namespace + " based on " + xsdSystemId, e);
		}

	}

	public void listTypes() {
		try {
			// Find an XMLSchema loader instance
//			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
//			XSImplementation implementation = (XSImplementation) registry.getDOMImplementation("XS-Loader");
			XSImplementation implementation = new XSImplementationImpl();
			XSLoader loader = implementation.createXSLoader(null);

			// Load the XML Schema
			List<String> systemIds = new ArrayList<>();
			for (Source source : sources) {
				systemIds.add(source.getSystemId());
			}
			StringList sl = new StringListImpl(systemIds.toArray(new String[systemIds.size()]), systemIds.size());
			XSModel xsModel = loader.loadURIList(sl);

			// Convert top level complex type definitions to node types
			log.debug("\n## TYPES");
			XSNamedMap map = xsModel.getComponents(XSConstants.TYPE_DEFINITION);
			for (int i = 0; i < map.getLength(); i++) {
				XSTypeDefinition tDef = (XSTypeDefinition) map.item(i);
				log.debug(tDef);
			}
			// Convert local (anonymous) complex type defs found in top level
			// element declarations
			log.debug("\n## ELEMENTS");
			map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i = 0; i < map.getLength(); i++) {
				XSElementDeclaration eDec = (XSElementDeclaration) map.item(i);
				XSTypeDefinition tDef = eDec.getTypeDefinition();
				log.debug(eDec + ", " + tDef);
			}
			log.debug("\n## ATTRIBUTES");
			map = xsModel.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
			for (int i = 0; i < map.getLength(); i++) {
				XSAttributeDeclaration eDec = (XSAttributeDeclaration) map.item(i);
				XSTypeDefinition tDef = eDec.getTypeDefinition();
				log.debug(eDec.getNamespace() + ":" + eDec.getName() + ", " + tDef);
			}
		} catch (ClassCastException | XSException e) {
			throw new RuntimeException(e);
		}

	}

	public Map<String, String> getPrefixes() {
		return prefixes;
	}

	public List<Source> getSources() {
		return sources;
	}

	public Schema getSchema() {
		return schema;
	}

}
