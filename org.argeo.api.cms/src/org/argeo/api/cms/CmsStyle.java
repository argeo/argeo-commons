package org.argeo.api.cms;

/** Can be applied to {@link Enum}s in order to generate (CSS) class names. */
public interface CmsStyle {
	String name();

	/** @deprecated use {@link #style()} instead. */
	@Deprecated
	default String toStyleClass() {
		return style();
	}

	default String style() {
		String classPrefix = getClassPrefix();
		return "".equals(classPrefix) ? name() : classPrefix + "-" + name();
	}

	default String getClassPrefix() {
		return "";
	}

}
