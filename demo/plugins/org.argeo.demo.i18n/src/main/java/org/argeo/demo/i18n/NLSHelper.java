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

import org.eclipse.osgi.util.NLS;

/**
 * thanks to {@link http
 * ://eclipsesource.com/en/info/rcp-rap-single-sourcing-guideline/}
 */
public abstract class NLSHelper {
	private final static NLSHelper IMPL;

	static {
		IMPL = (NLSHelper) ImplementationLoader.newInstance(NLSHelper.class);
	}

	public static NLS getMessages(String bundleName,
			@SuppressWarnings("rawtypes") Class clazz) {
		return (NLS) IMPL.internalGetMessages(bundleName, clazz);
	}

	protected abstract Object internalGetMessages(String bundleName,
			@SuppressWarnings("rawtypes") Class clazz);
}
