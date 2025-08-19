package org.argeo.cms.ssh.cli;

import static org.argeo.api.acr.CrAttributeType.BOOLEAN;

import org.argeo.api.acr.CrAttributeType;
import org.argeo.api.cli.ShortOpt;
import org.argeo.api.cli.ValuedOpt;

public enum SshOpt implements ValuedOpt, ShortOpt {
	port,//
	;

	@Override
	public char shortOpt() {
		return switch (this) {
		case port -> 'p';
		default -> 0;
		};
	}

	public CrAttributeType type() {
		return switch (this) {
		case port -> CrAttributeType.INTEGER;
		default -> BOOLEAN;
		};
	}

	@Override
	public Object defaultValue() {
		return switch (this) {
		case port -> Integer.valueOf(22);
		default -> null;
		};
	}

}
