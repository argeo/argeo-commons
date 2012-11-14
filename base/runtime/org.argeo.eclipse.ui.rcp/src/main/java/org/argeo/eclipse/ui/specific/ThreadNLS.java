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
package org.argeo.eclipse.ui.specific;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.argeo.util.LocaleUtils;
import org.eclipse.osgi.util.NLS;

/** NLS attached to a given thread */
public class ThreadNLS<T extends NLS> extends InheritableThreadLocal<T> {
	public final static String DEFAULT_BUNDLE_LOCATION = "/properties/plugin";

	private final String bundleLocation;

	private Class<T> type;
	private Boolean utf8 = false;

	public ThreadNLS(String bundleLocation, Class<T> type, Boolean utf8) {
		this.bundleLocation = bundleLocation;
		this.type = type;
		this.utf8 = utf8;
	}

	public ThreadNLS(Class<T> type) {
		this(DEFAULT_BUNDLE_LOCATION, type, false);
	}

	@Override
	protected T initialValue() {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleLocation,
				LocaleUtils.threadLocale.get(), type.getClassLoader());
		T result;
		try {
			NLS.initializeMessages(bundleLocation, type);
			Constructor<T> constructor = type.getConstructor();
			constructor.setAccessible(true);
			result = constructor.newInstance();
			final Field[] fieldArray = type.getDeclaredFields();
			for (int i = 0; i < fieldArray.length; i++) {
				int modifiers = fieldArray[i].getModifiers();
				if (String.class.isAssignableFrom(fieldArray[i].getType())
						&& Modifier.isPublic(modifiers)
						&& !Modifier.isStatic(modifiers)) {
					try {
						String value = bundle
								.getString(fieldArray[i].getName());
						byte[] bytes = value.getBytes();

						String forcedValue;
						if (utf8)
							forcedValue = new String(bytes, "UTF8");
						else
							forcedValue = value;
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
			}
			return result;
		} catch (final Exception ex) {
			throw new IllegalStateException(ex.getMessage());
		}
	}

}
