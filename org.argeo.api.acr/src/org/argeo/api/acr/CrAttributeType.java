package org.argeo.api.acr;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * Minimal standard attribute types that MUST be supported. All related classes
 * belong to java.base and can be implicitly derived form a given
 * <code>String</code>.
 */
public enum CrAttributeType {
	BOOLEAN(Boolean.class, W3C_XML_SCHEMA_NS_URI, "boolean", new BooleanFormatter()), //
	INTEGER(Integer.class, W3C_XML_SCHEMA_NS_URI, "integer", new IntegerFormatter()), //
	LONG(Long.class, W3C_XML_SCHEMA_NS_URI, "long", new LongFormatter()), //
	DOUBLE(Double.class, W3C_XML_SCHEMA_NS_URI, "double", new DoubleFormatter()), //
	// we do not support short and float, like recent additions to Java
	// (e.g. optional primitives)
	DATE_TIME(Instant.class, W3C_XML_SCHEMA_NS_URI, "dateTime", new InstantFormatter()), //
	UUID(UUID.class, ArgeoNamespace.CR_NAMESPACE_URI, "uuid", new UuidFormatter()), //
	QNAME(QName.class, W3C_XML_SCHEMA_NS_URI, "QName", new QNameFormatter()), //
	ANY_URI(URI.class, W3C_XML_SCHEMA_NS_URI, "anyUri", new UriFormatter()), //
	STRING(String.class, W3C_XML_SCHEMA_NS_URI, "string", new StringFormatter()), //
	;

	private final Class<?> clss;
	private final AttributeFormatter<?> formatter;

	private final ContentName qName;

	private <T> CrAttributeType(Class<T> clss, String namespaceUri, String localName, AttributeFormatter<T> formatter) {
		this.clss = clss;
		this.formatter = formatter;

		qName = new ContentName(namespaceUri, localName, RuntimeNamespaceContext.getNamespaceContext());
	}

	public QName getQName() {
		return qName;
	}

	public Class<?> getClss() {
		return clss;
	}

	public AttributeFormatter<?> getFormatter() {
		return formatter;
	}

//	@Override
//	public String getDefaultPrefix() {
//		if (equals(UUID))
//			return CrName.CR_DEFAULT_PREFIX;
//		else
//			return "xs";
//	}
//
//	@Override
//	public String getNamespaceURI() {
//		if (equals(UUID))
//			return CrName.CR_NAMESPACE_URI;
//		else
//			return XMLConstants.W3C_XML_SCHEMA_NS_URI;
//	}

	/** Default parsing procedure from a String to an object. */
	public static Object parse(String str) {
		return parse(RuntimeNamespaceContext.getNamespaceContext(), str);
	}

	/** Default parsing procedure from a String to an object. */
	public static Object parse(NamespaceContext namespaceContext, String str) {
		if (str == null)
			throw new IllegalArgumentException("String cannot be null");
		// order IS important
		try {
			if (str.length() == 4 || str.length() == 5)
				return BOOLEAN.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return INTEGER.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return LONG.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return DOUBLE.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return DATE_TIME.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			if (str.length() == 36)
				return UUID.getFormatter().parse(namespaceContext, str);
		} catch (IllegalArgumentException e) {
			// silent
		}

		// CURIE
		if (str.startsWith("[") && str.endsWith("]")) {
			try {
				if (str.indexOf(":") >= 0) {
					QName qName = (QName) QNAME.getFormatter().parse(namespaceContext, str);
					return (java.net.URI) ANY_URI.getFormatter().parse(qName.getNamespaceURI() + qName.getLocalPart());
				}
			} catch (IllegalArgumentException e) {
				// silent
			}
		}

		try {
			if (str.indexOf(":") >= 0) {
				QName qName = (QName) QNAME.getFormatter().parse(namespaceContext, str);
				// note: this QName may not be valid
				// note: CURIE should be explicitly defined with surrounding brackets
				return qName;
			}
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			java.net.URI uri = (java.net.URI) ANY_URI.getFormatter().parse(namespaceContext, str);
			if (uri.getScheme() != null)
				return uri;
			String path = uri.getPath();
			if (path.indexOf('/') >= 0)
				return uri;
			// if it is not clearly a path, we will consider it as a string
			// because their is no way to distinguish between 'any_string'
			// and 'any_file_name'.
			// Note that providing ./any_file_name would result in an equivalent URI
		} catch (IllegalArgumentException e) {
			// silent
		}

		// TODO support QName as a type? It would require a NamespaceContext
		// see https://www.oreilly.com/library/view/xml-schema/0596002521/re91.html

		// default
		return STRING.getFormatter().parse(namespaceContext, str);
	}

	/**
	 * Cast well know java types based on {@link Object#toString()} of the provided
	 * object.
	 * 
	 */
	public static <T> Optional<T> cast(Class<T> clss, Object value) {
		return cast(RuntimeNamespaceContext.getNamespaceContext(), clss, value);
	}

	/**
	 * Cast well know java types based on {@link Object#toString()} of the provided
	 * object.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> cast(NamespaceContext namespaceContext, Class<T> clss, Object value) {
		// if value is null, optional is empty
		if (value == null)
			return Optional.empty();

		// if a default has been explicitly requested by passing Object.class
		// we parse the related String
		if (clss.isAssignableFrom(Object.class)) {
			return Optional.of((T) parse(value.toString()));
		}

		// if value can be cast directly, let's do it
		if (value.getClass().isAssignableFrom(clss)) {
			return Optional.of(((T) value));
		}

		// let's cast between various numbers (possibly losing precision)
		if (value instanceof Number number) {
			if (Long.class.isAssignableFrom(clss))
				return Optional.of((T) (Long) number.longValue());
			else if (Integer.class.isAssignableFrom(clss))
				return Optional.of((T) (Integer) number.intValue());
			else if (Double.class.isAssignableFrom(clss))
				return Optional.of((T) (Double) number.doubleValue());
		}

		// let's now try with the string representation
		String strValue = value instanceof String ? (String) value : value.toString();

		if (String.class.isAssignableFrom(clss)) {
			return Optional.of((T) strValue);
		}
		if (java.util.UUID.class.isAssignableFrom(clss)) {
			return Optional.of((T) java.util.UUID.fromString(strValue));
		}
		if (QName.class.isAssignableFrom(clss)) {
			return Optional.of((T) NamespaceUtils.parsePrefixedName(namespaceContext, strValue));
		}
		// Numbers
		else if (Long.class.isAssignableFrom(clss)) {
			if (value instanceof Long)
				return Optional.of((T) value);
			return Optional.of((T) Long.valueOf(strValue));
		} else if (Integer.class.isAssignableFrom(clss)) {
			if (value instanceof Integer)
				return Optional.of((T) value);
			return Optional.of((T) Integer.valueOf(strValue));
		} else if (Double.class.isAssignableFrom(clss)) {
			if (value instanceof Double)
				return Optional.of((T) value);
			return Optional.of((T) Double.valueOf(strValue));
		}

		// let's now try to parse the string representation to a well-known type
		Object parsedValue = parse(strValue);
		if (parsedValue.getClass().isAssignableFrom(clss)) {
			return Optional.of(((T) value));
		}
		throw new ClassCastException("Cannot convert " + value.getClass() + " to " + clss);
	}

	/** Utility to convert a data: URI to bytes. */
	public static byte[] bytesFromDataURI(URI uri) {
		if (!"data".equals(uri.getScheme()))
			throw new IllegalArgumentException("URI must have 'data' as a scheme");
		String schemeSpecificPart = uri.getSchemeSpecificPart();
		int commaIndex = schemeSpecificPart.indexOf(',');
		String prefix = schemeSpecificPart.substring(0, commaIndex);
		List<String> info = Arrays.asList(prefix.split(";"));
		if (!info.contains("base64"))
			throw new IllegalArgumentException("URI must specify base64");

		String base64Str = schemeSpecificPart.substring(commaIndex + 1);
		return Base64.getDecoder().decode(base64Str);

	}

	/** Utility to convert bytes to a data: URI. */
	public static URI bytesToDataURI(byte[] arr) {
		String base64Str = Base64.getEncoder().encodeToString(arr);
		try {
			final String PREFIX = "data:application/octet-stream;base64,";
			return new URI(PREFIX + base64Str);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot serialize bytes a Base64 data URI", e);
		}

	}

	static class BooleanFormatter implements AttributeFormatter<Boolean> {

		/**
		 * @param str must be exactly equals to either 'true' or 'false' (different
		 *            contract than {@link Boolean#parseBoolean(String)}.
		 */
		@Override
		public Boolean parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException {
			if ("true".equals(str))
				return Boolean.TRUE;
			if ("false".equals(str))
				return Boolean.FALSE;
			throw new IllegalArgumentException("Argument is neither 'true' or 'false' : " + str);
		}
	}

	static class IntegerFormatter implements AttributeFormatter<Integer> {
		@Override
		public Integer parse(NamespaceContext namespaceContext, String str) throws NumberFormatException {
			return Integer.parseInt(str);
		}
	}

	static class LongFormatter implements AttributeFormatter<Long> {
		@Override
		public Long parse(NamespaceContext namespaceContext, String str) throws NumberFormatException {
			return Long.parseLong(str);
		}
	}

	static class DoubleFormatter implements AttributeFormatter<Double> {

		@Override
		public Double parse(NamespaceContext namespaceContext, String str) throws NumberFormatException {
			return Double.parseDouble(str);
		}
	}

	static class InstantFormatter implements AttributeFormatter<Instant> {

		@Override
		public Instant parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException {
			try {
				return Instant.parse(str);
			} catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Cannot parse '" + str + "' as an instant", e);
			}
		}
	}

	static class UuidFormatter implements AttributeFormatter<UUID> {

		@Override
		public UUID parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException {
			return java.util.UUID.fromString(str);
		}
	}

	static class UriFormatter implements AttributeFormatter<URI> {

		@Override
		public URI parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException {
			try {
				return new URI(str);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Cannot parse " + str + " as an URI.", e);
			}
		}

	}

	static class QNameFormatter implements AttributeFormatter<QName> {

		@Override
		public QName parse(NamespaceContext namespaceContext, String str) throws IllegalArgumentException {
			return NamespaceUtils.parsePrefixedName(namespaceContext, str);
		}

	}

	static class StringFormatter implements AttributeFormatter<String> {

		@Override
		public String parse(NamespaceContext namespaceContext, String str) {
			return str;
		}

		@Override
		public String format(String obj) {
			return obj;
		}

	}

}
