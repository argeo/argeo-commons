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
package org.argeo.jcr.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.security.SimplePrincipal;

/** Apply authorizations to a JCR repository. */
public class JcrAuthorizations implements Runnable {
	private final static Log log = LogFactory.getLog(JcrAuthorizations.class);

	private Repository repository;
	private String workspace = null;

	/**
	 * key := privilege1,privilege2/path/to/node<br/>
	 * value := group1,group2,user1
	 */
	private Map<String, String> principalPrivileges = new HashMap<String, String>();

	public void run() {
		Session session = null;
		try {
			session = repository.login(workspace);
			initAuthorizations(session);
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot set authorizations "
					+ principalPrivileges + " on repository " + repository, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	/** @deprecated call {@link #run()} instead. */
	@Deprecated
	public void init() {
		run();
	}

	protected void initAuthorizations(Session session)
			throws RepositoryException {
		AccessControlManager acm = session.getAccessControlManager();

		for (String privileges : principalPrivileges.keySet()) {
			String path = null;
			int slashIndex = privileges.indexOf('/');
			if (slashIndex == 0) {
				throw new ArgeoException("Privilege " + privileges
						+ " badly formatted it starts with /");
			} else if (slashIndex > 0) {
				path = privileges.substring(slashIndex);
				privileges = privileges.substring(0, slashIndex);
			}

			if (path == null)
				path = "/";

			List<Privilege> privs = new ArrayList<Privilege>();
			for (String priv : privileges.split(",")) {
				privs.add(acm.privilegeFromName(priv));
			}

			String principalNames = principalPrivileges.get(privileges);
			for (String principalName : principalNames.split(",")) {
				Principal principal = getOrCreatePrincipal(session,
						principalName);
				JcrUtils.addPrivileges(session, path, principal, privs);
			}
		}

		if (log.isDebugEnabled())
			log.debug("All authorizations applied on workspace "
					+ session.getWorkspace().getName());
	}

	/**
	 * Returns a {@link SimplePrincipal}, does not check whether it exists since
	 * such capabilities is not provided by the standard JCR API. Can be
	 * overridden to provide smarter handling
	 */
	protected Principal getOrCreatePrincipal(Session session,
			String principalName) throws RepositoryException {
		return new SimplePrincipal(principalName);
	}

	// public static void addPrivileges(Session session, Principal principal,
	// String path, List<Privilege> privs) throws RepositoryException {
	// AccessControlManager acm = session.getAccessControlManager();
	// // search for an access control list
	// AccessControlList acl = null;
	// AccessControlPolicyIterator policyIterator = acm
	// .getApplicablePolicies(path);
	// if (policyIterator.hasNext()) {
	// while (policyIterator.hasNext()) {
	// AccessControlPolicy acp = policyIterator
	// .nextAccessControlPolicy();
	// if (acp instanceof AccessControlList)
	// acl = ((AccessControlList) acp);
	// }
	// } else {
	// AccessControlPolicy[] existingPolicies = acm.getPolicies(path);
	// for (AccessControlPolicy acp : existingPolicies) {
	// if (acp instanceof AccessControlList)
	// acl = ((AccessControlList) acp);
	// }
	// }
	//
	// if (acl != null) {
	// acl.addAccessControlEntry(principal,
	// privs.toArray(new Privilege[privs.size()]));
	// acm.setPolicy(path, acl);
	// session.save();
	// if (log.isDebugEnabled()) {
	// StringBuffer buf = new StringBuffer("");
	// for (int i = 0; i < privs.size(); i++) {
	// if (i != 0)
	// buf.append(',');
	// buf.append(privs.get(i).getName());
	// }
	// log.debug("Added privilege(s) '" + buf + "' to '"
	// + principal.getName() + "' on " + path
	// + " from workspace '"
	// + session.getWorkspace().getName() + "'");
	// }
	// } else {
	// throw new ArgeoException("Don't know how to apply  privileges "
	// + privs + " to " + principal + " on " + path
	// + " from workspace '" + session.getWorkspace().getName()
	// + "'");
	// }
	// }

	@Deprecated
	public void setGroupPrivileges(Map<String, String> groupPrivileges) {
		this.principalPrivileges = groupPrivileges;
	}

	public void setPrincipalPrivileges(Map<String, String> principalPrivileges) {
		this.principalPrivileges = principalPrivileges;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

}
