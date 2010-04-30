package org.argeo.server.mvc;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.ServerSerializer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;

/**
 * Returns a {@link SerializingView} based on the underlying.
 */
public class SerializingViewResolver extends AbstractCachingViewResolver {
	private final static Log log = LogFactory
			.getLog(SerializingViewResolver.class);

	private ServerSerializer serializer;

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		if (log.isTraceEnabled())
			log.trace("viewName=" + viewName);
		return new SerializingView(viewName, locale, serializer);
	}

	public void setSerializer(ServerSerializer serializer) {
		this.serializer = serializer;
	}

}
