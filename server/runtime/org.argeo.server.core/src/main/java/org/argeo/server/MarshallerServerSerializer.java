/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	@SuppressWarnings("restriction")
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
