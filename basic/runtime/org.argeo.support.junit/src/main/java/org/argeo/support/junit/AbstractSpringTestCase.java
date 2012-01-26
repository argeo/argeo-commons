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

package org.argeo.support.junit;

import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/** Helper for tests using a Spring application context. */
public abstract class AbstractSpringTestCase extends TestCase {
	protected final Log log = LogFactory.getLog(getClass());
	private ConfigurableApplicationContext context;

	/**
	 * Gets (and creates if necessary) the application context to use. Default
	 * implementation uses a class path xml application context and calls
	 * {@link #getApplicationContextLocation()}.
	 */
	protected ConfigurableApplicationContext getContext() {
		if (context == null) {
			context = new ClassPathXmlApplicationContext(
					getApplicationContextLocation());
			if (getIsStartContext())
				context.start();
		}
		return context;
	}

	/** Whether the context should be started after being created. */
	protected Boolean getIsStartContext() {
		return false;
	}

	/** Returns a bean from the underlying context */
	@SuppressWarnings(value = { "unchecked" })
	protected <T> T getBean(String beanId) {
		return (T) getContext().getBean(beanId);
	}

	protected <T> T getBean(Class<? extends T> clss) {
		T bean = loadSingleFromContext(getContext(), clss);
		if (bean == null) {
			throw new RuntimeException("Cannot retrieve a unique bean of type "
					+ clss);
		} else {
			return bean;
		}
	}

	/**
	 * The location of the application to load. The default implementation
	 * returns <i>applicationContext.xml</i> found in the same package as the
	 * test.
	 */
	protected String getApplicationContextLocation() {
		return inPackage("applicationContext.xml");
	}

	/**
	 * Prefixes the package of the class after converting the '.' to '/' in
	 * order to have a resource path.
	 */
	protected String inPackage(String suffix) {
		String prefix = getClass().getPackage().getName().replace('.', '/');
		return prefix + '/' + suffix;
	}

	@SuppressWarnings(value = { "unchecked" })
	protected <T> T loadSingleFromContext(ListableBeanFactory context,
			Class<T> clss) {
		Map<String, T> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, clss, false, false);
		if (beans.size() == 1) {
			return beans.values().iterator().next();
		} else if (beans.size() > 1) {
			if (log.isDebugEnabled()) {
				log
						.debug(("Found more that one bean for type " + clss
								+ ": " + beans.keySet()));
			}
			return null;
		} else {
			return null;
		}
	}

}
