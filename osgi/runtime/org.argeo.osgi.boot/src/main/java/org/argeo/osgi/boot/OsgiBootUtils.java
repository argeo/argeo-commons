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

/** Utilities, mostly related to logging. */
public class OsgiBootUtils {

	public static void info(Object obj) {
		System.out.println("# OSGiBOOT      # " + obj);
	}

	public static void debug(Object obj) {
		System.out.println("# OSGiBOOT DBG  # " + obj);
	}

	public static void warn(Object obj) {
		System.out.println("# OSGiBOOT WARN # " + obj);
		// Because of a weird bug under Windows when starting it in a forked VM
		// if (System.getProperty("os.name").contains("Windows"))
		// System.out.println("# WARN " + obj);
		// else
		// System.err.println("# WARN " + obj);
	}

	/**
	 * Gets a property value
	 * 
	 * @return null when defaultValue is ""
	 */
	public static String getProperty(String name, String defaultValue) {
		final String value;
		if (defaultValue != null)
			value = System.getProperty(name, defaultValue);
		else
			value = System.getProperty(name);

		if (value == null || value.equals(""))
			return null;
		else
			return value;
	}

	public static String getProperty(String name) {
		return getProperty(name, null);
	}

}
