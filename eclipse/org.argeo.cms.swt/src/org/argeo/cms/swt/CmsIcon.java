package org.argeo.cms.swt;

import org.argeo.api.cms.CmsTheme;
import org.eclipse.swt.graphics.Image;

/** Can be applied to {@link Enum}s in order to generated {@link Image}s. */
public interface CmsIcon {
	String name();

	default Image getSmallIcon(CmsTheme theme) {
		return ((CmsSwtTheme) theme).getIcon(name(), getSmallIconSize());
	}

	default Image getBigIcon(CmsTheme theme) {
		return ((CmsSwtTheme) theme).getIcon(name(), getBigIconSize());
	}

	default Integer getSmallIconSize() {
		return 16;
	}

	default Integer getBigIconSize() {
		return 32;
	}
}
