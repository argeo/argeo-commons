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
package org.argeo.demo.i18n;

import java.text.MessageFormat;

/**
 * This class enable single sourcing between RAP and RCP. For this to run
 * correctly, following conventions must be respected:
 * <ul>
 * <li>Given the fact that a common interface named Xxx is defined in package
 * aa.bb.cc, corresponding implementation named XxxImpl must be found in package
 * aa.bb.cc.specific of both RAP and RCP UI bundles.
 * 
 * thanks to {@link http
 * ://eclipsesource.com/en/info/rcp-rap-single-sourcing-guideline/}, chapter 7
 */

public class ImplementationLoader {
	// private final static Log log = LogFactory
	// .getLog(ImplementationLoader.class);

	public static Object newInstance(
			@SuppressWarnings("rawtypes") final Class type) {
		String name = type.getName();
		// manually construct the implementation name for the given interface,
		// assuming that convention have been respected.
		String cName = type.getCanonicalName();
		String pName = cName.substring(0, cName.lastIndexOf('.') + 1);
		String sName = cName.substring(cName.lastIndexOf('.') + 1);
		String implName = pName + "specific." + sName + "Impl";
		// String implName = cName + "Impl";
		Object result = null;
		try {
			result = type.getClassLoader().loadClass(implName).newInstance();
		} catch (Throwable throwable) {
			String txt = "Could not load implementation for {0}";
			String msg = MessageFormat.format(txt, new Object[] { name });
			throw new RuntimeException(msg, throwable);
		}
		return result;
	}
}