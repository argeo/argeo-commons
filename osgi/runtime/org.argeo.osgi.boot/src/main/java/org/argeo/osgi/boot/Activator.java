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
package org.argeo.osgi.boot;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See <a
 * href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class Activator implements BundleActivator {

	public void start(final BundleContext bundleContext) throws Exception {
		// admin thread
		Thread adminThread = new AdminThread(bundleContext);
		adminThread.start();

		// bootstrap
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap();
	}

	public void stop(BundleContext context) throws Exception {
	}
}
