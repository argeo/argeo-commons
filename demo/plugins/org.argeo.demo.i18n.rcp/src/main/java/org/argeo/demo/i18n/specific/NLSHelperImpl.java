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
package org.argeo.demo.i18n.specific;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.argeo.demo.i18n.NLSHelper;

public class NLSHelperImpl extends NLSHelper {

	protected Object internalGetMessages(String bundleName, Class clazz) {

		ClassLoader loader = clazz.getClassLoader();
		// test
		// Locale currentLocale = new Locale("fr");
		// ResourceBundle bundle = ResourceBundle.getBundle(bundleName,
		// currentLocale, loader);
		// test end
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName,
				Locale.getDefault(), loader);
		return internalGet(bundle, clazz);
	}

	private Object internalGet(ResourceBundle bundle, Class clazz) {

		Object result;
		try {
			Constructor constructor = clazz.getDeclaredConstructor(null);
			constructor.setAccessible(true);
			result = constructor.newInstance(null);
		} catch (final Exception ex) {
			throw new IllegalStateException(ex.getMessage());
		}
		final Field[] fieldArray = clazz.getDeclaredFields();
		for (int i = 0; i < fieldArray.length; i++) {
			try {
				int modifiers = fieldArray[i].getModifiers();
				if (String.class.isAssignableFrom(fieldArray[i].getType())
						&& Modifier.isPublic(modifiers)
						&& !Modifier.isStatic(modifiers)) {
					try {
						String value = bundle
								.getString(fieldArray[i].getName());
						byte[] bytes = value.getBytes();
						String forcedValue = new String(bytes, "UTF8");
						if (value != null) {
							fieldArray[i].setAccessible(true);
							fieldArray[i].set(result, forcedValue);
						}
					} catch (final MissingResourceException mre) {
						fieldArray[i].setAccessible(true);
						fieldArray[i].set(result, "");
						mre.printStackTrace();
					}
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
}
