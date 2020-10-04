package org.argeo.cms.ui.util;

import org.argeo.cms.ui.CmsTheme;
import org.eclipse.swt.graphics.Image;

/** Can be applied to {@link Enum}s in order to generated {@link Image}s. */
public interface CmsIcon {
	String name();

	default Image getSmallIcon(CmsTheme theme) {
		return theme.getIcon(name(), getSmallIconSize());
	}

	default Image getBigIcon(CmsTheme theme) {
		return theme.getIcon(name(), getBigIconSize());
	}

	default Integer getSmallIconSize() {
		return 16;
	}

	default Integer getBigIconSize() {
		return 32;
	}
}
