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

package org.argeo.security;

import java.util.List;
import java.util.Map;

/** Abstraction for a user. */
public interface ArgeoUser {
	public String getUsername();

	@Deprecated
	public Map<String, UserNature> getUserNatures();

	/** Implementation should refuse to add new user natures via this method. */
	@Deprecated
	public void updateUserNatures(Map<String, UserNature> userNatures);

	public List<String> getRoles();

	public String getPassword();
}
