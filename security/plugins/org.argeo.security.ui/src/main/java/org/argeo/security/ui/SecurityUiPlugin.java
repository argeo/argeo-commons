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
package org.argeo.security.ui;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.argeo.security.ui.dialogs.DefaultLoginDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator class controls the plug-in life cycle
 */
public class SecurityUiPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.argeo.security.ui"; //$NON-NLS-1$

	public final static String CONTEXT_KEYRING = "KEYRING";

	private CallbackHandler defaultCallbackHandler;
	private ServiceRegistration defaultCallbackHandlerReg;

	private static SecurityUiPlugin plugin;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		defaultCallbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException {
				DefaultLoginDialog dialog = new DefaultLoginDialog();
				dialog.handle(callbacks);
			}
		};
		defaultCallbackHandlerReg = context.registerService(
				CallbackHandler.class.getName(), defaultCallbackHandler, null);
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		defaultCallbackHandlerReg.unregister();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static SecurityUiPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
