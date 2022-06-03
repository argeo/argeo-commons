package org.argeo.cms.acr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

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
import org.argeo.api.cms.CmsLog;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Register content types. */
class TypesManager {
	private final static CmsLog log = CmsLog.getLog(TypesManager.class);
	private Map<String, String> prefixes = new TreeMap<>();

	// immutable factories
	private SchemaFactory schemaFactory;

	/** Schema sources. */
	private List<Source> sources = new ArrayList<>();

	// cached
	private Schema schema;
	DocumentBuilderFactory documentBuilderFactory;
	private XSModel xsModel;
	private NavigableSet<QName> types;

	private boolean validating = true;

	public TypesManager() {
		schemaFactory = SchemaFactory.newDefaultInstance();

		// types
		types = new TreeSet<>((qn1, qn2) -> {
			if (Objects.equals(qn1.getNamespaceURI(), qn2.getNamespaceURI())) {// same namespace
				return qn1.getLocalPart().compareTo(qn2.getLocalPart());
			} else {
				return qn1.getNamespaceURI().compareTo(qn2.getNamespaceURI());
			}
		});

	}

	public synchronized void init() {
//		prefixes.put(CrName.CR_DEFAULT_PREFIX, CrName.CR_NAMESPACE_URI);
//		prefixes.put("basic", CrName.CR_NAMESPACE_URI);
//		prefixes.put("owner", CrName.CR_NAMESPACE_URI);
//		prefixes.put("posix", CrName.CR_NAMESPACE_URI);

		for (CmsContentTypes cs : CmsContentTypes.values()) {
			StreamSource source = new StreamSource(cs.getResource().toExternalForm());
			sources.add(source);
			if (prefixes.containsKey(cs.getDefaultPrefix()))
				throw new IllegalStateException("Prefix " + cs.getDefaultPrefix() + " is already mapped with "
						+ prefixes.get(cs.getDefaultPrefix()));
			prefixes.put(cs.getDefaultPrefix(), cs.getNamespace());
		}

		reload();
	}

	public synchronized void registerTypes(String defaultPrefix, String namespace, String xsdSystemId) {
		if (prefixes.containsKey(defaultPrefix))
			throw new IllegalStateException(
					"Prefix " + defaultPrefix + " is already mapped with " + prefixes.get(defaultPrefix));
		prefixes.put(defaultPrefix, namespace);

		sources.add(new StreamSource(xsdSystemId));
		reload();
	}

	public Set<QName> listTypes() {
// TODO cache it?
		return types;
	}

	private synchronized void reload() {
		try {
			// schema
			schema = schemaFactory.newSchema(sources.toArray(new Source[sources.size()]));

			// document builder factory
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilderFactory.setXIncludeAware(true);
			documentBuilderFactory.setSchema(getSchema());
			documentBuilderFactory.setValidating(validating);

			// XS model
			// TODO use JVM implementation?
//			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
//			XSImplementation implementation = (XSImplementation) registry.getDOMImplementation("XS-Loader");
			XSImplementation xsImplementation = new XSImplementationImpl();
			XSLoader xsLoader = xsImplementation.createXSLoader(null);
			List<String> systemIds = new ArrayList<>();
			for (Source source : sources) {
				systemIds.add(source.getSystemId());
			}
			StringList sl = new StringListImpl(systemIds.toArray(new String[systemIds.size()]), systemIds.size());
			xsModel = xsLoader.loadURIList(sl);

			// types
			XSNamedMap map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i = 0; i < map.getLength(); i++) {
				XSElementDeclaration eDec = (XSElementDeclaration) map.item(i);
				QName type = new QName(eDec.getNamespace(), eDec.getName());
				types.add(type);
			}
		} catch (XSException | SAXException e) {
			throw new IllegalStateException("Cannot relaod types");
		}
	}

	public DocumentBuilder newDocumentBuilder() {
		try {
			DocumentBuilder dBuilder = documentBuilderFactory.newDocumentBuilder();
			dBuilder.setErrorHandler(new ErrorHandler() {

				@Override
				public void warning(SAXParseException exception) throws SAXException {
					log.warn(exception);
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					log.error(exception);
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					log.error(exception);
				}
			});
			return dBuilder;
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("Cannot create document builder", e);
		}
	}

	public void printTypes() {
		try {

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

	public void validate(Source source) throws IOException {
		if (!validating)
			return;
		Validator validator;
		synchronized (this) {
			validator = schema.newValidator();
		}
		try {
			validator.validate(source);
		} catch (SAXException e) {
			throw new IllegalArgumentException("Provided source is not valid", e);
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
