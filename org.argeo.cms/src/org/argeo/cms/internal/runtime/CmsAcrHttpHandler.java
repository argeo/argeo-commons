package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.internal.http.RemoteAuthHttpExchange;
import org.argeo.util.StreamUtils;
import org.argeo.util.dav.DavServerHandler;
import org.argeo.util.http.HttpResponseStatus;
import org.argeo.util.http.HttpServerUtils;

import com.sun.net.httpserver.HttpExchange;

public class CmsAcrHttpHandler extends DavServerHandler {
	private ProvidedRepository contentRepository;

	@Override
	protected void handleGET(HttpExchange exchange) {
		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new RemoteAuthHttpExchange(exchange));
		String relativePath = HttpServerUtils.relativize(exchange);
		Content content = session.get(ContentUtils.ROOT_SLASH + relativePath);
		Optional<Long> size = content.get(CrName.size, Long.class);
		try (InputStream in = content.open(InputStream.class)) {
			exchange.sendResponseHeaders(HttpResponseStatus.OK.getStatusCode(), size.orElse(0l));
			StreamUtils.copy(in, exchange.getResponseBody());
		} catch (IOException e) {
			throw new RuntimeException("Cannot process " + relativePath, e);
		}
	}

	public void setContentRepository(ProvidedRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

}
