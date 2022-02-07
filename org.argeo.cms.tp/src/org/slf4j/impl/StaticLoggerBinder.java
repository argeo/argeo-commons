package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
	public static final String REQUESTED_API_VERSION = "1.7";

	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	private final ILoggerFactory loggerFactory;

	protected StaticLoggerBinder() {
		loggerFactory = new SystemLoggerFactory();
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public String getLoggerFactoryClassStr() {
		return SystemLoggerFactory.class.getName();
	}

	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	static class SystemLoggerFactory implements ILoggerFactory {

		@Override
		public org.slf4j.Logger getLogger(String name) {
			SystemLoggingAdapter logger = SystemLoggingAdapter.getLog(name);
			return new ArgeoLogger(name, logger);
		}

	}

}
