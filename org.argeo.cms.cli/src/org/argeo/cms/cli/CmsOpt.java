package org.argeo.cms.cli;

import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.cli.ValuedOpt;

public enum CmsOpt implements ValuedOpt {
	connect, //
	;

	@Override
	public CrAttributeType type() {
		return switch (this) {
		case connect -> CrAttributeType.ANY_URI;
		default -> CrAttributeType.BOOLEAN;
		};
	}

}
