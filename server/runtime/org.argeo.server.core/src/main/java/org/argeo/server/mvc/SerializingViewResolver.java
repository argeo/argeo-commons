/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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
