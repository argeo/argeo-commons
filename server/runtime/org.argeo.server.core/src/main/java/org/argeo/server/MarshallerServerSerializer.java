package org.argeo.server;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;

import org.argeo.ArgeoException;
import org.springframework.oxm.Marshaller;

public class MarshallerServerSerializer implements ServerSerializer, Serializer {
	private Marshaller marshaller;
	private String contentTypeCharset = "UTF-8";

	public void serialize(Object obj, HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/xml;charset=" + contentTypeCharset);
		try {
			serialize(obj, response.getWriter());
		} catch (IOException e) {
			throw new ArgeoException("Cannot serialize " + obj, e);
		}
	}

	public void serialize(Object obj, Writer writer) {
		try {
			StreamResult result = new StreamResult(writer);
			marshaller.marshal(obj, result);
		} catch (Exception e) {
			throw new ArgeoException("Cannot serialize " + obj, e);
		}
	}

	@Deprecated
	public void serialize(Writer writer, Object obj) {
		serialize(obj, writer);
	}

	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public void setContentTypeCharset(String contentTypeCharset) {
		this.contentTypeCharset = contentTypeCharset;
	}

}
