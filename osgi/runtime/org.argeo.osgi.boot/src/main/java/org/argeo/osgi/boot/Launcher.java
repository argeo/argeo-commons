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

package org.argeo.osgi.boot;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

/** Command line interface. */
public class Launcher {

	public static void main(String[] args) {
		// Try to load system properties
		String systemPropertiesFilePath = OsgiBootUtils
				.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_SYSTEM_PROPERTIES_FILE);
		if (systemPropertiesFilePath != null) {
			FileInputStream in;
			try {
				in = new FileInputStream(systemPropertiesFilePath);
				System.getProperties().load(in);
			} catch (IOException e1) {
				throw new RuntimeException(
						"Cannot load system properties from "
								+ systemPropertiesFilePath, e1);
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// silent
				}
			}
		}

		// Start main class
		startMainClass();

		// Start Equinox
		BundleContext bundleContext = null;
		try {
			bundleContext = EclipseStarter.startup(args, null);
		} catch (Exception e) {
			throw new RuntimeException("Cannot start Equinox.", e);
		}

		// OSGi bootstrap
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap();
	}

	protected static void startMainClass() {
		String className = OsgiBootUtils
				.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPCLASS);
		if (className == null)
			return;

		String line = System.getProperty(OsgiBoot.PROP_ARGEO_OSGI_BOOT_APPARGS,
				"");

		String[] uiArgs = readArgumentsFromLine(line);

		try {
			// Launch main method using reflection
			Class clss = Class.forName(className);
			Class[] mainArgsClasses = new Class[] { uiArgs.getClass() };
			Object[] mainArgs = { uiArgs };
			Method mainMethod = clss.getMethod("main", mainArgsClasses);
			mainMethod.invoke(null, mainArgs);
		} catch (Exception e) {
			throw new RuntimeException("Cannot start main class.", e);
		}

	}

	/**
	 * Transform a line into an array of arguments, taking "" as single
	 * arguments. (nested \" are not supported)
	 */
	private static String[] readArgumentsFromLine(String lineOrig) {
		String line = lineOrig.trim();// remove trailing spaces
		List args = new Vector();
		StringBuffer curr = new StringBuffer("");
		boolean inQuote = false;
		char[] arr = line.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			switch (c) {
			case '\"':
				inQuote = !inQuote;
				break;
			case ' ':
				if (!inQuote) {// otherwise, no break: goes to default
					if (curr.length() > 0) {
						args.add(curr.toString());
						curr = new StringBuffer("");
					}
					break;
				}
			default:
				curr.append(c);
				break;
			}
		}

		// Add last arg
		if (curr.length() > 0) {
			args.add(curr.toString());
			curr = null;
		}

		String[] res = new String[args.size()];
		for (int i = 0; i < args.size(); i++) {
			res[i] = args.get(i).toString();
		}
		return res;
	}

}
