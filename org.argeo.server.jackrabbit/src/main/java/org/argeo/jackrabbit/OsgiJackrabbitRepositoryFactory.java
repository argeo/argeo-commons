/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.jackrabbit;

import java.util.Hashtable;
import java.util.Properties;

import javax.jcr.Repository;

import org.osgi.framework.BundleContext;

/**
 * OSGi-aware Jackrabbit repository factory which can retrieve/publish
 * {@link Repository} as OSGi services.
 */
public class OsgiJackrabbitRepositoryFactory extends
		JackrabbitRepositoryFactory {
	private BundleContext bundleContext;

	protected void publish(String alias, Repository repository,
			Properties properties) {
		if (bundleContext != null) {
			// do not modify reference
			Hashtable<String, String> props = new Hashtable<String, String>();
			props.putAll(props);
			props.put(JCR_REPOSITORY_ALIAS, alias);
			bundleContext.registerService(Repository.class.getName(),
					repository, props);
		}
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
