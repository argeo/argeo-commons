package org.argeo.cms.integration;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.argeo.api.cms.CmsLog;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Serialisable wrapper of a {@link Throwable}. */
public class CmsExceptionsChain {
	public final static CmsLog log = CmsLog.getLog(CmsExceptionsChain.class);

	private List<SystemException> exceptions = new ArrayList<>();

	public CmsExceptionsChain() {
		super();
	}

	public CmsExceptionsChain(Throwable exception) {
		writeException(exception);
		if (log.isDebugEnabled())
			log.error("Exception chain", exception);
	}

	public String toJsonString(ObjectMapper objectMapper) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Cannot write system exceptions " + toString(), e);
		}
	}

	public void writeAsJson(ObjectMapper objectMapper, Writer writer) {
		try {
			JsonGenerator jg = objectMapper.writerWithDefaultPrettyPrinter().getFactory().createGenerator(writer);
			jg.writeObject(this);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot write system exceptions " + toString(), e);
		}
	}

	public void writeAsJson(ObjectMapper objectMapper, HttpServletResponse resp) {
		try {
			resp.setContentType("application/json");
			resp.setStatus(500);
			writeAsJson(objectMapper, resp.getWriter());
		} catch (IOException e) {
			throw new IllegalStateException("Cannot write system exceptions " + toString(), e);
		}
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

//	public static void main(String[] args) throws Exception {
//		try {
//			try {
//				try {
//					testDeeper();
//				} catch (Exception e) {
//					throw new Exception("Less deep exception", e);
//				}
//			} catch (Exception e) {
//				throw new RuntimeException("Top exception", e);
//			}
//		} catch (Exception e) {
//			CmsExceptionsChain vjeSystemErrors = new CmsExceptionsChain(e);
//			ObjectMapper objectMapper = new ObjectMapper();
//			System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vjeSystemErrors));
//			System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(e));
//			e.printStackTrace();
//		}
//	}
//
//	static void testDeeper() throws Exception {
//		throw new IllegalStateException("Deep exception");
//	}

}
