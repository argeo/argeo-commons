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
package org.argeo.security.ui.rap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** Configure Equinox login context from the bundle context. */
public class SecureRapActivator implements BundleActivator {
	public final static String ID = "org.argeo.security.ui.rap";

	private BundleContext bundleContext;
	private static SecureRapActivator activator = null;

	public void start(BundleContext bundleContext) throws Exception {
		activator = this;
		this.bundleContext = bundleContext;
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		activator = null;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public static SecureRapActivator getActivator() {
		return activator;
	}
}
