package org.argeo.api.gcr;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Minimal standard attribute types that MUST be supported. All related classes
 * belong to java.base and can be implicitly derived form a given
 * <code>String<code>.
 */
public enum StandardAttributeType {
	BOOLEAN(Boolean.class, new BooleanFormatter()), //
	INTEGER(Integer.class, new IntegerFormatter()), //
	LONG(Long.class, new LongFormatter()), //
	DOUBLE(Double.class, new DoubleFormatter()), //
	// we do not support short and float, like recent additions to Java
	// (e.g. optional primitives)
	INSTANT(Instant.class, new InstantFormatter()), //
	UUID(UUID.class, new UuidFormatter()), //
	URI(URI.class, new UriFormatter()), //
	STRING(String.class, new StringFormatter()), //
	;

	private <T> StandardAttributeType(Class<T> clss, AttributeFormatter<T> formatter) {
		this.clss = clss;
		this.formatter = formatter;
	}

	private final Class<?> clss;
	private final AttributeFormatter<?> formatter;

	public Class<?> getClss() {
		return clss;
	}

	public AttributeFormatter<?> getFormatter() {
		return formatter;
	}

	public static Object parse(String str) {
		if (str == null)
			throw new IllegalArgumentException("String cannot be null");
		// order IS important
		try {
			if (str.length() == 4 || str.length() == 5)
				return BOOLEAN.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return INTEGER.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return LONG.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return DOUBLE.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			return INSTANT.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			if (str.length() == 36)
				return UUID.getFormatter().parse(str);
		} catch (IllegalArgumentException e) {
			// silent
		}
		try {
			java.net.URI uri = (java.net.URI) URI.getFormatter().parse(str);
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

		// default
		return STRING.getFormatter().parse(str);
	}

	static class BooleanFormatter implements AttributeFormatter<Boolean> {

		/**
		 * @param str must be exactly equals to either 'true' or 'false' (different
		 *            contract than {@link Boolean#parseBoolean(String)}.
		 */
		@Override
		public Boolean parse(String str) throws IllegalArgumentException {
			if ("true".equals(str))
				return Boolean.TRUE;
			if ("false".equals(str))
				return Boolean.FALSE;
			throw new IllegalArgumentException("Argument is neither 'true' or 'false' : " + str);
		}
	}

	static class IntegerFormatter implements AttributeFormatter<Integer> {
		@Override
		public Integer parse(String str) throws NumberFormatException {
			return Integer.parseInt(str);
		}
	}

	static class LongFormatter implements AttributeFormatter<Long> {
		@Override
		public Long parse(String str) throws NumberFormatException {
			return Long.parseLong(str);
		}
	}

	static class DoubleFormatter implements AttributeFormatter<Double> {

		@Override
		public Double parse(String str) throws NumberFormatException {
			return Double.parseDouble(str);
		}
	}

	static class InstantFormatter implements AttributeFormatter<Instant> {

		@Override
		public Instant parse(String str) throws IllegalArgumentException {
			try {
				return Instant.parse(str);
			} catch (DateTimeParseException e) {
				throw new IllegalArgumentException("Cannot parse '" + str + "' as an instant", e);
			}
		}
	}

	static class UuidFormatter implements AttributeFormatter<UUID> {

		@Override
		public UUID parse(String str) throws IllegalArgumentException {
			return java.util.UUID.fromString(str);
		}
	}

	static class UriFormatter implements AttributeFormatter<URI> {

		@Override
		public URI parse(String str) throws IllegalArgumentException {
			try {
				return new URI(str);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Cannot parse " + str + " as an URI.", e);
			}
		}

	}

	static class StringFormatter implements AttributeFormatter<String> {

		@Override
		public String parse(String str) {
			return str;
		}

		@Override
		public String format(String obj) {
			return obj;
		}

	}

}
