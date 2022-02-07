package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;

class ArgeoLogger extends MarkerIgnoringBase {
	private static final long serialVersionUID = -7719157836932627307L;
	private final SystemLoggingAdapter log;

	protected ArgeoLogger(String name, SystemLoggingAdapter log) {
		this.name = name;
		this.log = log;
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	public void trace(String msg) {
		log.trace(msg);

	}

	@Override
	public void trace(String format, Object... arguments) {
		log.trace(format, arguments);

	}

	@Override
	public void trace(String msg, Throwable t) {
		log.trace(msg, t);

	}

	@Override
	public void debug(String msg) {
		log.debug(msg);

	}

	@Override
	public void debug(String format, Object... arguments) {
		log.debug(format, arguments);

	}

	@Override
	public void debug(String msg, Throwable t) {
		log.debug(msg, t);

	}

	@Override
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}

	@Override
	public void info(String msg) {
		log.info(msg);

	}

	@Override
	public void info(String format, Object... arguments) {
		log.info(format, arguments);
	}

	@Override
	public void info(String msg, Throwable t) {
		log.info(msg, t);

	}

	@Override
	public boolean isWarnEnabled() {
		return log.isWarnEnabled();
	}

	@Override
	public void warn(String msg) {
		log.warn(msg);

	}

	@Override
	public void warn(String format, Object... arguments) {
		log.warn(format, arguments);

	}

	@Override
	public void warn(String msg, Throwable t) {
		log.warn(msg, t);

	}

	@Override
	public boolean isErrorEnabled() {

		return log.isErrorEnabled();
	}

	@Override
	public void error(String msg) {
		log.error(msg);

	}

	@Override
	public void error(String format, Object... arguments) {
		log.error(format, arguments);

	}

	@Override
	public void error(String msg, Throwable t) {
		log.error(msg, t);

	}

	@Override
	public void trace(String format, Object arg) {
		trace(format, new Object[] { arg });

	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		trace(format, new Object[] { arg1, arg2 });
	}

	@Override
	public void debug(String format, Object arg) {
		debug(format, new Object[] { arg });
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		debug(format, new Object[] { arg1, arg2 });
	}

	@Override
	public void info(String format, Object arg) {
		info(format, new Object[] { arg });
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		info(format, new Object[] { arg1, arg2 });
	}

	@Override
	public void warn(String format, Object arg) {
		warn(format, new Object[] { arg });
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		warn(format, new Object[] { arg1, arg2 });
	}

	@Override
	public void error(String format, Object arg) {
		error(format, new Object[] { arg });
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		error(format, new Object[] { arg1, arg2 });
	}

}