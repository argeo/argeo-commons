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
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ArgeoUiPlugin extends AbstractUIPlugin implements ILogListener {
	public static final String PLUGIN_ID = "org.argeo.eclipse.ui";
	private final static Log log = LogFactory.getLog(ArgeoUiPlugin.class);
	// The shared instance
	private static ArgeoUiPlugin plugin;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		// weirdly, the start method is called twice...
		if (plugin == null) {
			plugin = this;
			Platform.addLogListener(this);
			log.debug("Eclipse logging now directed to standard logging");
		}
	}

	public void stop(BundleContext context) throws Exception {
		try {
			// weirdly, the stop method is called twice...
			if (plugin != null) {
				Platform.removeLogListener(this);
				log.debug("Eclipse logging not directed anymore to standard logging");
				plugin = null;
			}
		} finally {
			super.stop(context);
		}
	}

	/** Returns the shared instance */
	public static ArgeoUiPlugin getDefault() {
		return plugin;
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
