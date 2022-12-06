package org.argeo.cms.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialisable wrapper of a {@link Throwable}. typically to be written as XML
 * or JSON in a server error response.
 */
public class ExceptionsChain {
	private List<SystemException> exceptions = new ArrayList<>();

	public ExceptionsChain() {
	}

	public ExceptionsChain(Throwable exception) {
		writeException(exception);
	}

	/** recursive */
	protected void writeException(Throwable exception) {
		SystemException systemException = new SystemException(exception);
		exceptions.add(systemException);
		Throwable cause = exception.getCause();
		if (cause != null)
			writeException(cause);
	}

	public List<SystemException> getExceptions() {
		return exceptions;
	}

	public void setExceptions(List<SystemException> exceptions) {
		this.exceptions = exceptions;
	}

	/** An exception in the chain. */
	public static class SystemException {
		private String type;
		private String message;
		private List<String> stackTrace;

		public SystemException() {
		}

		public SystemException(Throwable exception) {
			this.type = exception.getClass().getName();
			this.message = exception.getMessage();
			this.stackTrace = new ArrayList<>();
			StackTraceElement[] elems = exception.getStackTrace();
			for (int i = 0; i < elems.length; i++)
				stackTrace.add("at " + elems[i].toString());
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public List<String> getStackTrace() {
			return stackTrace;
		}

		public void setStackTrace(List<String> stackTrace) {
			this.stackTrace = stackTrace;
		}

		@Override
		public String toString() {
			return "System exception: " + type + ", " + message + ", " + stackTrace;
		}

	}

	@Override
	public String toString() {
		return exceptions.toString();
	}
}
