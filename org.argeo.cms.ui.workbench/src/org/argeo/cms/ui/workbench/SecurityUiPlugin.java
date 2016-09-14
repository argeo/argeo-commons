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
package org.argeo.cms.ui.workbench;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.cms.widgets.auth.DefaultLoginDialog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator class controls the plug-in life cycle
 */
public class SecurityUiPlugin extends AbstractUIPlugin implements ILogListener {

	private final static Log log = LogFactory.getLog(SecurityUiPlugin.class);

	private ResourceBundle messages;

	private static BundleContext bundleContext;
	public static InheritableThreadLocal<Display> display = new InheritableThreadLocal<Display>() {

		@Override
		protected Display initialValue() {
			return Display.getCurrent();
		}
	};

	// The plug-in ID
	// 	public final static String PLUGIN_ID = "org.argeo.security.ui"; //$NON-NLS-1$
	public final static String PLUGIN_ID = "org.argeo.cms.ui.workbench"; //$NON-NLS-1$

	final static String CONTEXT_KEYRING = "KEYRING";

	private CallbackHandler defaultCallbackHandler;
	private ServiceRegistration<CallbackHandler> defaultCallbackHandlerReg;

	// The shared instance
	private static SecurityUiPlugin plugin;

	public void start(BundleContext context) throws Exception {
		super.start(context);

		bundleContext = context;

		defaultCallbackHandler = new DefaultCallbackHandler();
		defaultCallbackHandlerReg = context.registerService(
				CallbackHandler.class, defaultCallbackHandler, null);

		plugin = this;
		messages = ResourceBundle.getBundle(PLUGIN_ID + ".messages");
		Platform.addLogListener(this);
		if (log.isTraceEnabled())
			log.trace("Eclipse logging now directed to standard logging");

	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		defaultCallbackHandlerReg.unregister();
	}

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	// * Returns the shared instance
	// *
	// * @return the shared instance
	// */
	public static SecurityUiPlugin getDefault() {
		return plugin;
	}

	protected class DefaultCallbackHandler implements CallbackHandler {
		public void handle(final Callback[] callbacks) throws IOException,
				UnsupportedCallbackException {

			// if (display != null) // RCP
			Display displayToUse = display.get();
			if (displayToUse == null)// RCP
				displayToUse = Display.getDefault();
			displayToUse.syncExec(new Runnable() {
				public void run() {
					DefaultLoginDialog dialog = new DefaultLoginDialog(display
							.get().getActiveShell());
					try {
						dialog.handle(callbacks);
					} catch (IOException e) {
						throw new CmsException("Cannot open dialog", e);
					}
				}
			});
			// else {// RAP
			// DefaultLoginDialog dialog = new DefaultLoginDialog();
			// dialog.handle(callbacks);
			// }
		}

	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
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
