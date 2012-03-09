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
package org.argeo.security.ui.rcp;

import java.net.URL;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** Activator able to create {@link ILoginContext} */
public class SecureApplicationActivator implements BundleActivator {

	public final static String CONTEXT_REMOTE = "REMOTE";
	public final static String CONTEXT_NIX = "NIX";
	public final static String CONTEXT_WINDOWS = "WINDOWS";
	private static final String JAAS_CONFIG_FILE = "/META-INF/jaas_default.txt";

	private static BundleContext bundleContext;

	public void start(BundleContext bundleContext) throws Exception {
		SecureApplicationActivator.bundleContext = bundleContext;
	}

	public void stop(BundleContext context) throws Exception {
	}

	static ILoginContext createLoginContext(String context) {
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		return LoginContextFactory.createContext(context, configUrl);
	}
}
