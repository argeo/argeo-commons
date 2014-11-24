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
package org.argeo.eclipse.ui.jcr;

import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class JcrUiPlugin extends AbstractUIPlugin {
	private final static Log log = LogFactory.getLog(JcrUiPlugin.class);

	public final static String ID = "org.argeo.eclipse.ui.jcr";

	private ResourceBundle messages;

	private static JcrUiPlugin plugin;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		messages = ResourceBundle.getBundle("org.argeo.eclipse.ui.jcr");
	}

	public static JcrUiPlugin getDefault() {
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
	 * case the ClientUiPlugin is not started (for JUnit tests, by instance)
	 */
	public static ResourceBundle getMessagesBundle() {
		if (getDefault() != null)
			// To avoid NPE
			return getDefault().messages;
		else
			return null;
	}
}
