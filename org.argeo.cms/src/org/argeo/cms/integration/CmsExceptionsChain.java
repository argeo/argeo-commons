package org.argeo.cms.integration;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.argeo.util.ExceptionsChain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Serialisable wrapper of a {@link Throwable}. */
public class CmsExceptionsChain extends ExceptionsChain {
	public CmsExceptionsChain() {
		super();
	}

	public CmsExceptionsChain(Throwable exception) {
		super(exception);
	}

	public String toJsonString(ObjectMapper objectMapper) {
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Cannot write system exceptions " + toString(), e);
		}
	}

	public void writeAsJson(ObjectMapper objectMapper, Writer writer) {
		try {
			JsonGenerator jg = objectMapper.getFactory().createGenerator(writer);
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

	public static void main(String[] args) throws Exception {
		try {
			try {
				try {
					testDeeper();
				} catch (Exception e) {
					throw new Exception("Less deep exception", e);
				}
			} catch (Exception e) {
				throw new RuntimeException("Top exception", e);
			}
		} catch (Exception e) {
			CmsExceptionsChain vjeSystemErrors = new CmsExceptionsChain(e);
			ObjectMapper objectMapper = new ObjectMapper();
			System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vjeSystemErrors));
			e.printStackTrace();
		}
	}

	static void testDeeper() throws Exception {
		throw new IllegalStateException("Deep exception");
	}

}
