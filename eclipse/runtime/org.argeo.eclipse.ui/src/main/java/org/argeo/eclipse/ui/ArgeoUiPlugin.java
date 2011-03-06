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

package org.argeo.eclipse.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArgeoUiPlugin extends AbstractUIPlugin implements ILogListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.argeo.eclipse.ui";

	private final static String SPRING_OSGI_EXTENDER = "org.springframework.osgi.extender";

	private final static Log log = LogFactory.getLog(ArgeoUiPlugin.class);

	// The shared instance
	private static ArgeoUiPlugin plugin;

	private BundleContext bundleContext;

	/**
	 * The constructor
	 */
	public ArgeoUiPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		bundleContext = context;

		Platform.addLogListener(this);
		log.debug("Eclipse logging now directed to standard logging");

		// Make sure that the Spring OSGi extender is started
		Bundle osgiExtBundle = Platform.getBundle(SPRING_OSGI_EXTENDER);
		if (osgiExtBundle != null)
			osgiExtBundle.start();
		else
			log.error("Spring OSGi Extender not found");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		Platform.removeLogListener(this);
		log.debug("Eclipse logging not directed anymore to standard logging");

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static ArgeoUiPlugin getDefault() {
		return plugin;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public void logging(IStatus status, String plugin) {
		Log pluginLog = LogFactory.getLog(plugin);
		Integer severity = status.getSeverity();
		if (severity == IStatus.ERROR)
			pluginLog.error(status.getMessage(), status.getException());
		else if (severity == IStatus.WARNING)
			pluginLog.warn(status.getMessage(), status.getException());
		else if (severity == IStatus.INFO)
			pluginLog.info(status.getMessage(), status.getException());
		else if (severity == IStatus.CANCEL)
			if (pluginLog.isDebugEnabled())
				pluginLog.debug(status.getMessage(), status.getException());

	}

}
