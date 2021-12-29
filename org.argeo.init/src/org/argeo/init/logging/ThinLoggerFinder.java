package org.argeo.init.logging;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;

/** Factory for Java system logging. */
public class ThinLoggerFinder extends LoggerFinder {
	private ThinLogging logging;

	public ThinLoggerFinder() {
		logging = new ThinLogging();
	}

	@Override
	public Logger getLogger(String name, Module module) {
		return logging.getLogger(name, module);
	}

}
