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
package org.argeo.security.jcr;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Manages data expected by the Argeo security model, such as user home and
 * profile.
 */
public interface JcrSecurityModel {
	/**
	 * To be called before user details are loaded. Make sure than any logged in
	 * user has a home directory with full access and a profile with information
	 * about him (read access)
	 * 
	 * @return the user profile, never null
	 */
	public Node sync(Session session, String username, List<String> roles);
}
