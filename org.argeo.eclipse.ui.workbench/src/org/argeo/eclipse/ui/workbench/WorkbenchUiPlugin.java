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
package org.argeo.eclipse.ui.workbench;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class WorkbenchUiPlugin extends AbstractUIPlugin implements ILogListener {
	private final static Log log = LogFactory.getLog(WorkbenchUiPlugin.class);
	private ResourceBundle messages;

	// The plug-in ID
	public static final String ID = "org.argeo.eclipse.ui.workbench"; //$NON-NLS-1$

	// The shared instance
	private static WorkbenchUiPlugin plugin;

	/**
	 * The constructor
	 */
	public WorkbenchUiPlugin() {
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
		// weirdly, the start method is called twice...
		// TODO check if it is still the case.
		if (plugin == null) {
			plugin = this;
			messages = ResourceBundle.getBundle(ID + ".messages");
			Platform.addLogListener(this);
			log.debug("Eclipse logging now directed to standard logging");
		} else
			log.warn("Trying to start an already started plugin.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			// weirdly, the stop method is called twice...
			// TODO check if it is still the case.
			if (plugin != null) {
				Platform.removeLogListener(this);
				log.debug("Eclipse logging not directed anymore to standard logging");
				plugin = null;
			} else
				log.warn("Trying to stop an already stopped plugin.");
		} finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static WorkbenchUiPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(ID, path);
	}

	/** Returns the internationalized label for the given key */
	public static String getMessage(String key) {
		try {
			return getDefault().messages.getString(key);
		} catch (NullPointerException npe) {
			log.warn(key + " not found.");
			return key;
		}
	}

	/**
	 * Gives access to the internationalization message bundle. Returns null in
	 * case this UiPlugin is not started (for JUnit tests, by instance)
	 */
	public static ResourceBundle getMessagesBundle() {
		if (getDefault() != null)
			// To avoid NPE
			return getDefault().messages;
		else
			return null;
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
