package org.argeo.cms.ui.util;

/** Can be applied to {@link Enum}s in order to generate (CSS) class names. */
public interface CmsStyle {
	String name();

	@Deprecated
	default String toStyleClass() {
		return getClassPrefix() + "-" + name();
	}

	default String style() {
		return getClassPrefix() + "-" + name();
	}

	default String getClassPrefix() {
		return "cms";
	}

}
