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
package org.argeo.cms.util;

import java.security.AccessController;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.argeo.ArgeoException;

/**
 * Retrieves information about the current user. Not an API, can change without
 * notice.
 */
class CurrentUserUtils {
	public final static String getUsername() {
		Subject subject = getSubject();
		if (subject == null)
			return null;
		Principal principal = subject.getPrincipals(X500Principal.class)
				.iterator().next();
		return principal.getName();

	}

	public final static Set<String> roles() {
		Set<String> roles = Collections.synchronizedSet(new HashSet<String>());
		// roles.add("ROLE_USER");
		Subject subject = getSubject();
		X500Principal userPrincipal = subject
				.getPrincipals(X500Principal.class).iterator().next();
		roles.add(userPrincipal.getName());
		for (Principal group : subject.getPrincipals(Group.class)) {
			roles.add(group.getName());
		}
		return roles;
	}

	public final static Subject getSubject() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null) {
			subject = CmsUtils.getCmsView().getSubject();
			if (subject == null)
				throw new ArgeoException("Not authenticated.");
		}
		return subject;
	}
}
