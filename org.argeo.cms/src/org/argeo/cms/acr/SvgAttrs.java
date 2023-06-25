package org.argeo.cms.acr;

import org.argeo.api.acr.QNamed;

/**
 * Some core SVG attributes, which are used to standardise generic concepts such
 * as width, height, etc.
 */
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
		return CmsContentNamespace.SVG.getNamespaceURI();
	}

	@Override
	public String getDefaultPrefix() {
		return CmsContentNamespace.SVG.getDefaultPrefix();
	}

}
