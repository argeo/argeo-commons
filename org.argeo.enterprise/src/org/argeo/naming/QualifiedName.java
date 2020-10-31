package org.argeo.naming;

/** Can be applied to {@link Enum}s in order to generate prefixed names. */
public interface QualifiedName {
	String name();

	default String getPrefix() {
		return null;
	}

	default String getNamespace() {
		return null;
	}

	default String property() {
		return qualified();
	}

	default String qualified() {
		String prefix = getPrefix();
		return prefix != null ? prefix + ":" + name() : name();
	}

	default String withNamespace() {
		String namespace = getNamespace();
		if (namespace == null)
			throw new UnsupportedOperationException("No namespace is specified for " + getClass());
		return "{" + namespace + "}" + name();
	}
}
