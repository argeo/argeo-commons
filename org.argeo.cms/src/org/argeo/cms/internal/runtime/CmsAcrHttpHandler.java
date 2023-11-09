package org.argeo.cms.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.DName;
import org.argeo.api.acr.RuntimeNamespaceContext;
import org.argeo.api.acr.spi.ProvidedRepository;
import org.argeo.api.cms.CmsConstants;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.dav.DavDepth;
import org.argeo.cms.dav.DavHttpHandler;
import org.argeo.cms.dav.DavPropfind;
import org.argeo.cms.dav.DavResponse;
import org.argeo.cms.http.HttpStatus;
import org.argeo.cms.http.RemoteAuthHttpExchange;
import org.argeo.cms.util.StreamUtils;

import com.sun.net.httpserver.HttpExchange;

/** A partial WebDav implementation based on ACR. */
public class CmsAcrHttpHandler extends DavHttpHandler {
	private ProvidedRepository contentRepository;

	@Override
	protected NamespaceContext getNamespaceContext(HttpExchange httpExchange, String path) {
		// TODO be smarter?
		return RuntimeNamespaceContext.getNamespaceContext();
	}

	@Override
	protected void handleGET(HttpExchange exchange, String path) throws IOException {
		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new RemoteAuthHttpExchange(exchange));
		if (!session.exists(path)) // not found
			throw new ContentNotFoundException(session, path);
		Content content = session.get(path);
		Optional<Long> size = content.get(DName.getcontentlength, Long.class);
		try (InputStream in = content.open(InputStream.class)) {
			exchange.sendResponseHeaders(HttpStatus.OK.getCode(), size.orElse(0l));
			StreamUtils.copy(in, exchange.getResponseBody());
		} catch (IOException e) {
			throw new RuntimeException("Cannot process " + path, e);
		}
	}

	@Override
	protected CompletableFuture<Void> handlePROPFIND(HttpExchange exchange, String path, DavPropfind davPropfind,
			Consumer<DavResponse> consumer) throws IOException {
		ContentSession session = RemoteAuthUtils.doAs(() -> contentRepository.get(),
				new RemoteAuthHttpExchange(exchange));
		if (!session.exists(path)) // not found
			throw new ContentNotFoundException(session, path);
		Content content = session.get(path);

		CompletableFuture<Void> published = new CompletableFuture<Void>();
		ForkJoinPool.commonPool().execute(() -> {
			publishDavResponses(content, davPropfind, consumer);
			published.complete(null);
		});
		return published;
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
				davResponse.getPropertyNames(HttpStatus.OK).add(entry.getKey());
				processMapEntry(davResponse, entry.getKey(), entry.getValue());
			}
			davResponse.getResourceTypes().addAll(content.getContentClasses());
		} else if (davPropfind.isPropname()) {
			for (QName key : content.keySet()) {
				davResponse.getPropertyNames(HttpStatus.OK).add(key);
			}
		} else {
			for (QName key : davPropfind.getProps()) {
				if (content.containsKey(key)) {
					davResponse.getPropertyNames(HttpStatus.OK).add(key);
					Object value = content.get(key);
					processMapEntry(davResponse, key, value);
				} else {
					davResponse.getPropertyNames(HttpStatus.NOT_FOUND).add(key);
				}
				if (DName.resourcetype.qName().equals(key)) {
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

	public void setContentRepository(ProvidedRepository contentRepository) {
		this.contentRepository = contentRepository;
	}

}
