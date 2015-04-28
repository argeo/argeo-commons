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
package org.argeo.security.ui;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.argeo.ArgeoException;
import org.argeo.cms.widgets.auth.DefaultLoginDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator class controls the plug-in life cycle
 */
public class SecurityUiPlugin implements BundleActivator {
	private static BundleContext bundleContext;
	public static InheritableThreadLocal<Display> display = new InheritableThreadLocal<Display>() {

		@Override
		protected Display initialValue() {
			return Display.getCurrent();
		}
	};

	// The plug-in ID
	public final static String PLUGIN_ID = "org.argeo.security.ui"; //$NON-NLS-1$

	final static String CONTEXT_KEYRING = "KEYRING";

	private CallbackHandler defaultCallbackHandler;
	private ServiceRegistration<CallbackHandler> defaultCallbackHandlerReg;

	public void start(BundleContext context) throws Exception {
		if (bundleContext != null)
			if (!bundleContext.equals(bundleContext))
				throw new ArgeoException(
						"Bundle context already set with a different value");
			else
				return;

		bundleContext = context;

		defaultCallbackHandler = new DefaultCallbackHandler();
		defaultCallbackHandlerReg = context.registerService(
				CallbackHandler.class, defaultCallbackHandler, null);
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		defaultCallbackHandlerReg.unregister();
	}

	public static BundleContext getBundleContext() {
		return bundleContext;
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
						throw new ArgeoException("Cannot open dialog", e);
					}
				}
			});
			// else {// RAP
			// DefaultLoginDialog dialog = new DefaultLoginDialog();
			// dialog.handle(callbacks);
			// }
		}

	}
}
