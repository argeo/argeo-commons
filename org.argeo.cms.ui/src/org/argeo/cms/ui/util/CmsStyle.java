package org.argeo.cms.ui.util;

/** Can be applied to {@link Enum}s in order to generated (CSS) class names. */
public interface CmsStyle {
	default String toStyleClass() {
		return getClassPrefix() + "-" + ((Enum<?>) this).name();
	}

	default String getClassPrefix() {
		return "cms";
	}
}
