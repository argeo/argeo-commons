package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.acr.ContentUtils;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.dav.DavDepth;
import org.argeo.cms.dav.DavMethod;
import org.argeo.cms.dav.DavPropfind;
import org.argeo.cms.dav.DavResponse;
import org.argeo.cms.dav.DavXmlElement;
import org.argeo.cms.dav.MultiStatusWriter;
import org.argeo.cms.internal.http.RemoteAuthHttpExchange;
import org.argeo.util.StreamUtils;
import org.argeo.util.http.HttpMethod;
import org.argeo.util.http.HttpResponseStatus;
import org.argeo.util.http.HttpServerUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class CmsAcrHttpHandler implements HttpHandler {
	private ProvidedRepository contentRepository;

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		if (DavMethod.PROPFIND.name().equals(method)) {
			handlePROPFIND(exchange);
		} else if (HttpMethod.GET.name().equals(method)) {
			handleGET(exchange);
		} else {
			throw new IllegalArgumentException("Unsupported method " + method);
		}

	}

	protected void handlePROPFIND(HttpExchange exchange) throws IOException {
		String relativePath = HttpServerUtils.relativize(exchange);

		DavDepth depth = DavDepth.fromHttpExchange(exchange);
		if (depth == null) {
			// default, as per http://www.webdav.org/specs/rfc4918.html#METHOD_PROPFIND
			depth = DavDepth.DEPTH_INFINITY;
		}

		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new RemoteAuthHttpExchange(exchange));

		String path = ContentUtils.ROOT_SLASH + relativePath;
		if (!session.exists(path)) {// not found
			exchange.sendResponseHeaders(HttpResponseStatus.NOT_FOUND.getCode(), -1);
			return;
		}
		Content content = session.get(path);

		CompletableFuture<Void> published = new CompletableFuture<Void>();

		try (InputStream in = exchange.getRequestBody()) {
			DavPropfind davPropfind = DavPropfind.load(depth, in);
			MultiStatusWriter msWriter = new MultiStatusWriter();
			ForkJoinPool.commonPool().execute(() -> {
				publishDavResponses(content, davPropfind, msWriter);
				published.complete(null);
			});
			exchange.sendResponseHeaders(HttpResponseStatus.MULTI_STATUS.getCode(), 0l);
			try (OutputStream out = exchange.getResponseBody()) {
				msWriter.process(session, out, published.minimalCompletionStage(), davPropfind.isPropname());
			}
		}
	}

	protected void publishDavResponses(Content content, DavPropfind davPropfind, Consumer<DavResponse> consumer) {
		publishDavResponse(content, davPropfind, consumer, 0);
	}

	protected void publishDavResponse(Content content, DavPropfind davPropfind, Consumer<DavResponse> consumer,
			int currentDepth) {
		DavResponse davResponse = new DavResponse();
		String href = CmsConstants.PATH_API_ACR + content.getPath();
		davResponse.setHref(href);
		if (content.hasContentClass(DName.collection))
			davResponse.setCollection(true);
		if (davPropfind.isAllprop()) {
			for (Map.Entry<QName, Object> entry : content.entrySet()) {
				davResponse.getPropertyNames().add(entry.getKey());
				processMapEntry(davResponse, entry.getKey(), entry.getValue());
			}
			davResponse.getResourceTypes().addAll(content.getContentClasses());
		} else if (davPropfind.isPropname()) {
			for (QName key : content.keySet()) {
				davResponse.getPropertyNames().add(key);
			}
		} else {
			for (QName key : davPropfind.getProps()) {
				if (content.containsKey(key)) {
					davResponse.getPropertyNames().add(key);
					Object value = content.get(key);
					processMapEntry(davResponse, key, value);
				}
				if (DavXmlElement.resourcetype.qName().equals(key)) {
					davResponse.getResourceTypes().addAll(content.getContentClasses());
				}
			}

		}

		consumer.accept(davResponse);

		// recurse only on collections
		if (content.hasContentClass(DName.collection)) {
			if (davPropfind.getDepth() == DavDepth.DEPTH_INFINITY
					|| (davPropfind.getDepth() == DavDepth.DEPTH_1 && currentDepth == 0)) {
				for (Content child : content) {
					publishDavResponse(child, davPropfind, consumer, currentDepth + 1);
				}
			}
		}
	}

	protected void processMapEntry(DavResponse davResponse, QName key, Object value) {
		// ignore content classes
		if (DName.resourcetype.qName().equals(key))
			return;
		String str;
		if (value instanceof Collection) {
			StringJoiner sj = new StringJoiner("\n");
			for (Object v : (Collection<?>) value) {
				sj.add(v.toString());
			}
			str = sj.toString();
		} else {
			str = value.toString();
		}
		davResponse.getProperties().put(key, str);

	}

	protected void handleGET(HttpExchange exchange) {
		String relativePath = HttpServerUtils.relativize(exchange);
		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new RemoteAuthHttpExchange(exchange));
		Content content = session.get(ContentUtils.ROOT_SLASH + relativePath);
		Optional<Long> size = content.get(DName.getcontentlength, Long.class);
		try (InputStream in = content.open(InputStream.class)) {
			exchange.sendResponseHeaders(HttpResponseStatus.OK.getCode(), size.orElse(0l));
			StreamUtils.copy(in, exchange.getResponseBody());
		} catch (IOException e) {
			throw new RuntimeException("Cannot process " + relativePath, e);
		}
	}

	public void setContentRepository(ProvidedRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

}
