package org.argeo.jcr.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
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

	/**
	 * key := privilege1,privilege2/path/to/node<br/>
	 * value := group1,group2,user1
	 */
	private Map<String, String> principalPrivileges = new HashMap<String, String>();

	public void run() {
		Session session = null;
		try {
			session = repository.login();
			initAuthorizations(session);
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
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
				addPrivileges(session, principal, path, privs);
			}
		}
		session.save();
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

	public static void addPrivileges(Session session, Principal principal,
			String path, List<Privilege> privs) throws RepositoryException {
		AccessControlManager acm = session.getAccessControlManager();
		// search for an access control list
		AccessControlList acl = null;
		AccessControlPolicyIterator policyIterator = acm
				.getApplicablePolicies(path);
		if (policyIterator.hasNext()) {
			while (policyIterator.hasNext()) {
				AccessControlPolicy acp = policyIterator
						.nextAccessControlPolicy();
				if (acp instanceof AccessControlList)
					acl = ((AccessControlList) acp);
			}
		} else {
			AccessControlPolicy[] existingPolicies = acm.getPolicies(path);
			for (AccessControlPolicy acp : existingPolicies) {
				if (acp instanceof AccessControlList)
					acl = ((AccessControlList) acp);
			}
		}

		if (acl != null) {
			acl.addAccessControlEntry(principal,
					privs.toArray(new Privilege[privs.size()]));
			acm.setPolicy(path, acl);
			if (log.isDebugEnabled())
				log.debug("Added privileges " + privs + " to " + principal
						+ " on " + path);
		} else {
			throw new ArgeoException("Don't know how to apply  privileges "
					+ privs + " to " + principal + " on " + path);
		}
	}

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

}
