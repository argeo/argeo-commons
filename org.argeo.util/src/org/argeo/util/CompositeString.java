package org.argeo.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/** A name that can be expressed with various conventions. */
public class CompositeString {
	public final static Character UNDERSCORE = Character.valueOf('_');
	public final static Character SPACE = Character.valueOf(' ');
	public final static Character DASH = Character.valueOf('-');

	private final String[] parts;

	// optimisation
	private final int hashCode;

	public CompositeString(String str) {
		Objects.requireNonNull(str, "String cannot be null");
		if ("".equals(str.trim()))
			throw new IllegalArgumentException("String cannot be empty");
		if (!str.equals(str.trim()))
			throw new IllegalArgumentException("String must be trimmed");
		this.parts = toParts(str);
		hashCode = hashCode(this.parts);
	}

	public String toString(char separator, boolean upperCase) {
		StringBuilder sb = null;
		for (String part : parts) {
			if (sb == null) {
				sb = new StringBuilder();
			} else {
				sb.append(separator);
			}
			sb.append(upperCase ? part.toUpperCase() : part);
		}
		return sb.toString();
	}

	public String toStringCaml(boolean firstCharUpperCase) {
		StringBuilder sb = null;
		for (String part : parts) {
			if (sb == null) {// first
				sb = new StringBuilder();
				sb.append(firstCharUpperCase ? Character.toUpperCase(part.charAt(0)) : part.charAt(0));
			} else {
				sb.append(Character.toUpperCase(part.charAt(0)));
			}

			if (part.length() > 1)
				sb.append(part.substring(1));
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof CompositeString))
			return false;

		CompositeString other = (CompositeString) obj;
		return Arrays.equals(parts, other.parts);
	}

	@Override
	public String toString() {
		return toString(DASH, false);
	}

	public static String[] toParts(String str) {
		Character separator = null;
		if (str.indexOf(UNDERSCORE) >= 0) {
			checkNo(str, SPACE);
			checkNo(str, DASH);
			separator = UNDERSCORE;
		} else if (str.indexOf(DASH) >= 0) {
			checkNo(str, SPACE);
			checkNo(str, UNDERSCORE);
			separator = DASH;
		} else if (str.indexOf(SPACE) >= 0) {
			checkNo(str, DASH);
			checkNo(str, UNDERSCORE);
			separator = SPACE;
		}

		List<String> res = new ArrayList<>();
		if (separator != null) {
			StringTokenizer st = new StringTokenizer(str, separator.toString());
			while (st.hasMoreTokens()) {
				res.add(st.nextToken().toLowerCase());
			}
		} else {
			// single
			String strLowerCase = str.toLowerCase();
			if (str.toUpperCase().equals(str) || strLowerCase.equals(str))
				return new String[] { strLowerCase };

			// CAML
			StringBuilder current = null;
			for (char c : str.toCharArray()) {
				if (Character.isUpperCase(c)) {
					if (current != null)
						res.add(current.toString());
					current = new StringBuilder();
				}
				if (current == null)// first char is lower case
					current = new StringBuilder();
				current.append(Character.toLowerCase(c));
			}
			res.add(current.toString());
		}
		return res.toArray(new String[res.size()]);
	}

	private static void checkNo(String str, Character c) {
		if (str.indexOf(c) >= 0) {
			throw new IllegalArgumentException("Only one kind of sperator is allowed");
		}
	}

	private static int hashCode(String[] parts) {
		int hashCode = 0;
		for (String part : parts) {
			hashCode = hashCode + part.hashCode();
		}
		return hashCode;
	}

	static boolean smokeTests() {
		CompositeString plainName = new CompositeString("NAME");
		assert "name".equals(plainName.toString());
		assert "NAME".equals(plainName.toString(UNDERSCORE, true));
		assert "name".equals(plainName.toString(UNDERSCORE, false));
		assert "name".equals(plainName.toStringCaml(false));
		assert "Name".equals(plainName.toStringCaml(true));

		CompositeString camlName = new CompositeString("myComplexName");

		assert new CompositeString("my-complex-name").equals(camlName);
		assert new CompositeString("MY_COMPLEX_NAME").equals(camlName);
		assert new CompositeString("My complex Name").equals(camlName);
		assert new CompositeString("MyComplexName").equals(camlName);

		assert "my-complex-name".equals(camlName.toString());
		assert "MY_COMPLEX_NAME".equals(camlName.toString(UNDERSCORE, true));
		assert "my_complex_name".equals(camlName.toString(UNDERSCORE, false));
		assert "myComplexName".equals(camlName.toStringCaml(false));
		assert "MyComplexName".equals(camlName.toStringCaml(true));

		return CompositeString.class.desiredAssertionStatus();
	}

	public static void main(String[] args) {
		System.out.println(smokeTests());
	}
}
