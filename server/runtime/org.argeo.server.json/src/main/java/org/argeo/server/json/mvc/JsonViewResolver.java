package org.argeo.server.json.mvc;

import java.util.Locale;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;

/**
 * Returns a {@link JsonView} based on the underlying. View name is the model
 * key of the JSON view.
 */
public class JsonViewResolver extends AbstractCachingViewResolver {
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		return new JsonView(viewName);
	}

}
