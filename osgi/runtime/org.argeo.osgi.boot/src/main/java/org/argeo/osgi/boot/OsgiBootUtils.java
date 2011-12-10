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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;

/** Utilities, mostly related to logging. */
public class OsgiBootUtils {
	/** ISO8601 (as per log4j) and difference to UTC */
	private static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss,SSS Z");

	public static void info(Object obj) {
		System.out.println("# OSGiBOOT      # " + dateFormat.format(new Date())
				+ " # " + obj);
	}

	public static void debug(Object obj) {
		System.out.println("# OSGiBOOT DBG  # " + dateFormat.format(new Date())
				+ " # " + obj);
	}

	public static void warn(Object obj) {
		System.out.println("# OSGiBOOT WARN # " + dateFormat.format(new Date())
				+ " # " + obj);
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

	public static String stateAsString(int state) {
		switch (state) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.STOPPING:
			return "STOPPING";
		default:
			return Integer.toString(state);
		}
	}

	/**
	 * @return ==0: versions are identical, <0: tested version is newer, >0:
	 *         currentVersion is newer.
	 */
	public static int compareVersions(String currentVersion,
			String testedVersion) {
		List cToks = new ArrayList();
		StringTokenizer cSt = new StringTokenizer(currentVersion, ".");
		while (cSt.hasMoreTokens())
			cToks.add(cSt.nextToken());
		List tToks = new ArrayList();
		StringTokenizer tSt = new StringTokenizer(currentVersion, ".");
		while (tSt.hasMoreTokens())
			tToks.add(tSt.nextToken());
	
		int comp = 0;
		comp: for (int i = 0; i < cToks.size(); i++) {
			if (tToks.size() <= i) {
				// equals until then, tested shorter
				comp = 1;
				break comp;
			}
	
			String c = (String) cToks.get(i);
			String t = (String) tToks.get(i);
	
			try {
				int cInt = Integer.parseInt(c);
				int tInt = Integer.parseInt(t);
				if (cInt == tInt)
					continue comp;
				else {
					comp = (cInt - tInt);
					break comp;
				}
			} catch (NumberFormatException e) {
				if (c.equals(t))
					continue comp;
				else {
					comp = c.compareTo(t);
					break comp;
				}
			}
		}
	
		if (comp == 0 && tToks.size() > cToks.size()) {
			// equals until then, current shorter
			comp = -1;
		}
	
		return comp;
	}

}
