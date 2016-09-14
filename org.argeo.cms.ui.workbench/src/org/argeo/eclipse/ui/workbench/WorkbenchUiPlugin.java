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

public class WorkbenchUiPlugin {
}

// /**
// * The activator class controls the plug-in life cycle
// */
// public class SecurityUiPlugin extends AbstractUIPlugin implements
// ILogListener {
// private final static Log log = LogFactory.getLog(SecurityUiPlugin.class);
// private ResourceBundle messages;
//
// // The plug-in ID
//	public static final String ID = "org.argeo.eclipse.ui.workbench"; //$NON-NLS-1$
//
// // The shared instance
// private static SecurityUiPlugin plugin;
//
// /**
// * The constructor
// */
// public SecurityUiPlugin() {
// }
//
// public void start(BundleContext context) throws Exception {
// super.start(context);
// plugin = this;
// messages = ResourceBundle.getBundle(ID + ".messages");
// Platform.addLogListener(this);
// if (log.isTraceEnabled())
// log.trace("Eclipse logging now directed to standard logging");
// }
//
// public void stop(BundleContext context) throws Exception {
// try {
// Platform.removeLogListener(this);
// if (log.isTraceEnabled())
// log.trace("Eclipse logging not directed anymore to standard logging");
// plugin = null;
// } finally {
// super.stop(context);
// }
// }
//
// /**
// * Returns the shared instance
// *
// * @return the shared instance
// */
// public static SecurityUiPlugin getDefault() {
// return plugin;
// }
//
// public static ImageDescriptor getImageDescriptor(String path) {
// return imageDescriptorFromPlugin(ID, path);
// }
//
// /** Returns the internationalized label for the given key */
// public static String getMessage(String key) {
// try {
// return getDefault().messages.getString(key);
// } catch (NullPointerException npe) {
// log.warn(key + " not found.");
// return key;
// }
// }
//
// /**
// * Gives access to the internationalization message bundle. Returns null in
// * case this UiPlugin is not started (for JUnit tests, by instance)
// */
// public static ResourceBundle getMessagesBundle() {
// if (getDefault() != null)
// // To avoid NPE
// return getDefault().messages;
// else
// return null;
// }
//
// public void logging(IStatus status, String plugin) {
// Log pluginLog = LogFactory.getLog(plugin);
// Integer severity = status.getSeverity();
// if (severity == IStatus.ERROR)
// pluginLog.error(status.getMessage(), status.getException());
// else if (severity == IStatus.WARNING)
// pluginLog.warn(status.getMessage(), status.getException());
// else if (severity == IStatus.INFO)
// pluginLog.info(status.getMessage(), status.getException());
// else if (severity == IStatus.CANCEL)
// if (pluginLog.isDebugEnabled())
// pluginLog.debug(status.getMessage(), status.getException());
// }
// }
