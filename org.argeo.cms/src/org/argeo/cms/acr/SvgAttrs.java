package org.argeo.cms.acr;

import org.argeo.api.acr.QNamed;

public enum SvgAttrs implements QNamed {
	/** */
	width,
	/** */
	height,
	/** */
	length,
	/** */
	unit,
	/** */
	dur,
	/** */
	direction,
	//
	;

	@Override
	public String getNamespace() {
		return CmsContentTypes.SVG_1_1.getNamespace();
	}

	@Override
	public String getDefaultPrefix() {
		return CmsContentTypes.SVG_1_1.getDefaultPrefix();
	}

}
