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
package org.argeo.jackrabbit;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.jcr.security.JcrAuthorizations;

/** Apply authorizations to a Jackrabbit repository. */
public class JackrabbitAuthorizations extends JcrAuthorizations {
	private final static Log log = LogFactory
			.getLog(JackrabbitAuthorizations.class);

	private List<String> groupPrefixes = new ArrayList<String>();

	@Override
	protected Principal getOrCreatePrincipal(Session session,
			String principalName) throws RepositoryException {
		UserManager um = ((JackrabbitSession) session).getUserManager();
		Authorizable authorizable = um.getAuthorizable(principalName);
		if (authorizable == null) {
			groupPrefixes: for (String groupPrefix : groupPrefixes) {
				if (principalName.startsWith(groupPrefix)) {
					authorizable = um.createGroup(principalName);
					log.info("Created group " + principalName);
					break groupPrefixes;
				}
			}
			if (authorizable == null)
				throw new ArgeoException("Authorizable " + principalName
						+ " not found");
		}
		return authorizable.getPrincipal();
	}

	public void setGroupPrefixes(List<String> groupsToCreate) {
		this.groupPrefixes = groupsToCreate;
	}
}
