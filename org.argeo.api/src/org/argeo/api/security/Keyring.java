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
package org.argeo.api.security;

import java.io.InputStream;

/**
 * Access to private (typically encrypted) data. The keyring is responsible for
 * retrieving the necessary credentials. <b>Experimental. This API may
 * change.</b>
 */
public interface Keyring {
	/**
	 * Returns the confidential information as chars. Must ask for it if it is
	 * not stored.
	 */
	public char[] getAsChars(String path);

	/**
	 * Returns the confidential information as a stream. Must ask for it if it
	 * is not stored.
	 */
	public InputStream getAsStream(String path);

	public void set(String path, char[] arr);

	public void set(String path, InputStream in);
}
