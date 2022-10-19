package org.argeo.cms.acr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
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
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSException;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.RuntimeNamespaceContext;
import org.argeo.api.cms.CmsLog;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Register content types. */
class TypesManager {
	private final static CmsLog log = CmsLog.getLog(TypesManager.class);
//	private Map<String, String> prefixes = new TreeMap<>();

	// immutable factories
	private SchemaFactory schemaFactory;

	/** Schema sources. */
	private List<Source> sources = new ArrayList<>();

	// cached
	private Schema schema;
	private DocumentBuilderFactory documentBuilderFactory;
	private XSModel xsModel;
	private SortedMap<QName, Map<QName, CrAttributeType>> types;

	private boolean validating = true;

	private final static boolean limited = false;

	public TypesManager() {
		schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		// types
		types = new TreeMap<>(NamespaceUtils.QNAME_COMPARATOR);

	}

	public void init() {
		for (CmsContentTypes cs : CmsContentTypes.values()) {
			if (cs.getResource() != null) {
				StreamSource source = new StreamSource(cs.getResource().toExternalForm());
				sources.add(source);
			}
			RuntimeNamespaceContext.register(cs.getNamespace(), cs.getDefaultPrefix());
		}

		reload();
	}

	public void registerTypes(String defaultPrefix, String namespace, String xsdSystemId) {
//		if (prefixes.containsKey(defaultPrefix))
//			throw new IllegalStateException(
//					"Prefix " + defaultPrefix + " is already mapped with " + prefixes.get(defaultPrefix));
//		prefixes.put(defaultPrefix, namespace);
		RuntimeNamespaceContext.register(namespace, defaultPrefix);

		if (xsdSystemId != null) {
			sources.add(new StreamSource(xsdSystemId));
			reload();
			log.debug(() -> "Registered types " + namespace + " from " + xsdSystemId);
		}
	}

	public Set<QName> listTypes() {
		return types.keySet();
	}

	public Map<QName, CrAttributeType> getAttributeTypes(QName type) {
		if (!types.containsKey(type))
			throw new IllegalArgumentException("Unkown type");
		return types.get(type);
	}

	private synchronized void reload() {
		try {
			// schema
			schema = schemaFactory.newSchema(sources.toArray(new Source[sources.size()]));

			// document builder factory
			// we force usage of Xerces for predictability
			documentBuilderFactory = limited ? DocumentBuilderFactory.newInstance() : new DocumentBuilderFactoryImpl();
			documentBuilderFactory.setNamespaceAware(true);
			if (!limited) {
				documentBuilderFactory.setXIncludeAware(true);
				documentBuilderFactory.setSchema(getSchema());
				documentBuilderFactory.setValidating(validating);
			}

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
//			XSNamedMap map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
//			for (int i = 0; i < map.getLength(); i++) {
//				XSElementDeclaration eDec = (XSElementDeclaration) map.item(i);
//				QName type = new QName(eDec.getNamespace(), eDec.getName());
//				types.add(type);
//			}
			collectTypes();
		} catch (XSException | SAXException e) {
			throw new IllegalStateException("Cannot reload types", e);
		}
	}

	private void collectTypes() {
		types.clear();
		// elements
		XSNamedMap topLevelElements = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
		for (int i = 0; i < topLevelElements.getLength(); i++) {
			XSElementDeclaration eDec = (XSElementDeclaration) topLevelElements.item(i);
			collectElementDeclaration("", eDec);
		}

		// types
		XSNamedMap topLevelTypes = xsModel.getComponents(XSConstants.TYPE_DEFINITION);
		for (int i = 0; i < topLevelTypes.getLength(); i++) {
			XSTypeDefinition tDef = (XSTypeDefinition) topLevelTypes.item(i);
			collectType(tDef, null, null);
		}

	}

	private void collectType(XSTypeDefinition tDef, String namespace, String nameHint) {
		if (tDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			XSComplexTypeDefinition ctDef = (XSComplexTypeDefinition) tDef;
			if (ctDef.getContentType() != XSComplexTypeDefinition.CONTENTTYPE_SIMPLE
					|| ctDef.getAttributeUses().getLength() > 0 || ctDef.getAttributeWildcard() != null) {
				collectComplexType("", null, ctDef);
			} else {
				throw new IllegalArgumentException("Unsupported type " + tDef.getTypeCategory());
			}
		}
	}

	private void collectComplexType(String prefix, QName parent, XSComplexTypeDefinition ctDef) {
		if (ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE) {

			// content with attributes and a string value

			XSSimpleTypeDefinition stDef = ctDef.getSimpleType();
			// QName name = new QName(stDef.getNamespace(), stDef.getName());
			// log.warn(prefix + "Simple " + ctDef + " - " + attributes);
//			System.err.println(prefix + "Simple from " + parent + " - " + attributes);
//
//			if (parentAttributes != null) {
//				for (QName attr : attributes.keySet()) {
//					if (!parentAttributes.containsKey(attr))
//						System.err.println(prefix + " - " + attr + " not available in parent");
//
//				}
//			}

		} else if (ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT
				|| ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED) {
			XSParticle p = ctDef.getParticle();

			collectParticle(prefix, p, false);
		} else if (ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_EMPTY) {
			// Parent only contains attributes
//			if (parent != null)
//				System.err.println(prefix + "Empty from " + parent + " - " + attributes);
//			if (parentAttributes != null) {
//				for (QName attr : attributes.keySet()) {
//					if (!parentAttributes.containsKey(attr))
//						System.err.println(prefix + " - " + attr + " not available in parent");
//
//				}
//			}
//			log.debug(prefix + "Empty " + ctDef.getNamespace() + ":" + ctDef.getName() + " - " + attributes);
		} else {
			throw new IllegalArgumentException("Unsupported type " + ctDef.getTypeCategory());
		}
	}

	private void collectParticle(String prefix, XSParticle particle, boolean multipleFromAbove) {
		boolean orderable = false;

		XSTerm term = particle.getTerm();

		if (particle.getMaxOccurs() == 0) {
			return;
		}

		boolean mandatory = false;
		if (particle.getMinOccurs() > 0) {
			mandatory = true;
		}

		boolean multiple = false;
		if (particle.getMaxOccurs() > 1 || particle.getMaxOccursUnbounded()) {
			multiple = true;
		}
		if (!multiple && multipleFromAbove)
			multiple = true;

		if (term.getType() == XSConstants.ELEMENT_DECLARATION) {
			XSElementDeclaration eDec = (XSElementDeclaration) term;

			collectElementDeclaration(prefix, eDec);
			// If this particle is a wildcard (an <xs:any> )then it
			// is converted into a node def.
		} else if (term.getType() == XSConstants.WILDCARD) {
			// TODO can be anything

			// If this particle is a model group (one of
			// <xs:sequence>, <xs:choice> or <xs:all>) then
			// it subparticles must be processed.
		} else if (term.getType() == XSConstants.MODEL_GROUP) {
			XSModelGroup mg = (XSModelGroup) term;

			if (mg.getCompositor() == XSModelGroup.COMPOSITOR_SEQUENCE) {
				orderable = true;
			}
			XSObjectList list = mg.getParticles();
			for (int i = 0; i < list.getLength(); i++) {
				XSParticle pp = (XSParticle) list.item(i);
				collectParticle(prefix + "  ", pp, multiple);
			}
		}
	}

	private void collectElementDeclaration(String prefix, XSElementDeclaration eDec) {
		QName name = new QName(eDec.getNamespace(), eDec.getName());
		XSTypeDefinition tDef = eDec.getTypeDefinition();

		XSComplexTypeDefinition ctDef = null;
		Map<QName, CrAttributeType> attributes = new HashMap<>();
		if (tDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
			XSSimpleTypeDefinition stDef = (XSSimpleTypeDefinition) tDef;
//			System.err.println(prefix + "Simple element " + name);
		} else if (tDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
			ctDef = (XSComplexTypeDefinition) tDef;
			if (ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE
					&& ctDef.getAttributeUses().getLength() == 0 && ctDef.getAttributeWildcard() == null) {
				XSSimpleTypeDefinition stDef = ctDef.getSimpleType();
//				System.err.println(prefix + "Simplified element " + name);
			} else {
				if (!types.containsKey(name)) {
//					System.out.println(prefix + "Element " + name);

					XSObjectList list = ctDef.getAttributeUses();
					for (int i = 0; i < list.getLength(); i++) {
						XSAttributeUse au = (XSAttributeUse) list.item(i);
						XSAttributeDeclaration ad = au.getAttrDeclaration();
						QName attrName = new QName(ad.getNamespace(), ad.getName());
						// Get the simple type def for this attribute
						XSSimpleTypeDefinition std = ad.getTypeDefinition();
						attributes.put(attrName, xsToCrType(std.getBuiltInKind()));
//						System.out.println(prefix + " - " + attrName + " = " + attributes.get(attrName));
					}
					// REGISTER
					types.put(name, attributes);
					if (ctDef != null)
						collectComplexType(prefix + " ", name, ctDef);
				}
			}
		}

	}

	private CrAttributeType xsToCrType(short kind) {
		CrAttributeType propertyType;
		switch (kind) {
		case XSConstants.ANYSIMPLETYPE_DT:
		case XSConstants.STRING_DT:
		case XSConstants.ID_DT:
		case XSConstants.ENTITY_DT:
		case XSConstants.NOTATION_DT:
		case XSConstants.NORMALIZEDSTRING_DT:
		case XSConstants.TOKEN_DT:
		case XSConstants.LANGUAGE_DT:
		case XSConstants.NMTOKEN_DT:
			propertyType = CrAttributeType.STRING;
			break;
		case XSConstants.BOOLEAN_DT:
			propertyType = CrAttributeType.BOOLEAN;
			break;
		case XSConstants.DECIMAL_DT:
		case XSConstants.FLOAT_DT:
		case XSConstants.DOUBLE_DT:
			propertyType = CrAttributeType.DOUBLE;
			break;
		case XSConstants.DURATION_DT:
		case XSConstants.DATETIME_DT:
		case XSConstants.TIME_DT:
		case XSConstants.DATE_DT:
		case XSConstants.GYEARMONTH_DT:
		case XSConstants.GYEAR_DT:
		case XSConstants.GMONTHDAY_DT:
		case XSConstants.GDAY_DT:
		case XSConstants.GMONTH_DT:
			propertyType = CrAttributeType.DATE_TIME;
			break;
		case XSConstants.HEXBINARY_DT:
		case XSConstants.BASE64BINARY_DT:
		case XSConstants.ANYURI_DT:
			propertyType = CrAttributeType.ANY_URI;
			break;
		case XSConstants.QNAME_DT:
		case XSConstants.NAME_DT:
		case XSConstants.NCNAME_DT:
			// TODO support QName?
			propertyType = CrAttributeType.STRING;
			break;
		case XSConstants.IDREF_DT:
			// TODO support references?
			propertyType = CrAttributeType.STRING;
			break;
		case XSConstants.INTEGER_DT:
		case XSConstants.NONPOSITIVEINTEGER_DT:
		case XSConstants.NEGATIVEINTEGER_DT:
		case XSConstants.LONG_DT:
		case XSConstants.INT_DT:
		case XSConstants.SHORT_DT:
		case XSConstants.BYTE_DT:
		case XSConstants.NONNEGATIVEINTEGER_DT:
		case XSConstants.UNSIGNEDLONG_DT:
		case XSConstants.UNSIGNEDINT_DT:
		case XSConstants.UNSIGNEDSHORT_DT:
		case XSConstants.UNSIGNEDBYTE_DT:
		case XSConstants.POSITIVEINTEGER_DT:
			propertyType = CrAttributeType.LONG;
			break;
		case XSConstants.LISTOFUNION_DT:
		case XSConstants.LIST_DT:
		case XSConstants.UNAVAILABLE_DT:
			propertyType = CrAttributeType.STRING;
			break;
		default:
			propertyType = CrAttributeType.STRING;
			break;
		}
		return propertyType;
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
			log.error(source + " is not valid " + e);
			// throw new IllegalArgumentException("Provided source is not valid", e);
		}
	}

//	public Map<String, String> getPrefixes() {
//		return prefixes;
//	}

	public List<Source> getSources() {
		return sources;
	}

	public Schema getSchema() {
		return schema;
	}

}
