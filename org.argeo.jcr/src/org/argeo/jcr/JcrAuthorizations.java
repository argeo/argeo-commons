package org.argeo.jcr;

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
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/** Apply authorizations to a JCR repository. */
public class JcrAuthorizations implements Runnable {
	// private final static Log log =
	// LogFactory.getLog(JcrAuthorizations.class);

	private Repository repository;
	private String workspace = null;

	private String securityWorkspace = "security";

	/**
	 * key := privilege1,privilege2/path/to/node<br/>
	 * value := group1,group2,user1
	 */
	private Map<String, String> principalPrivileges = new HashMap<String, String>();

	public void run() {
		String currentWorkspace = workspace;
		Session session = null;
		try {
			if (workspace != null && workspace.equals("*")) {
				session = repository.login();
				String[] workspaces = session.getWorkspace().getAccessibleWorkspaceNames();
				JcrUtils.logoutQuietly(session);
				for (String wksp : workspaces) {
					currentWorkspace = wksp;
					if (currentWorkspace.equals(securityWorkspace))
						continue;
					session = repository.login(currentWorkspace);
					initAuthorizations(session);
					JcrUtils.logoutQuietly(session);
				}
			} else {
				session = repository.login(workspace);
				initAuthorizations(session);
			}
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new JcrException(
					"Cannot set authorizations " + principalPrivileges + " on workspace " + currentWorkspace, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	protected void processWorkspace(String workspace) {
		Session session = null;
		try {
			session = repository.login(workspace);
			initAuthorizations(session);
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new JcrException(
					"Cannot set authorizations " + principalPrivileges + " on repository " + repository, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}
	}

	/** @deprecated call {@link #run()} instead. */
	@Deprecated
	public void init() {
		run();
	}

	protected void initAuthorizations(Session session) throws RepositoryException {
		AccessControlManager acm = session.getAccessControlManager();

		for (String privileges : principalPrivileges.keySet()) {
			String path = null;
			int slashIndex = privileges.indexOf('/');
			if (slashIndex == 0) {
				throw new IllegalArgumentException("Privilege " + privileges + " badly formatted it starts with /");
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
			try {
				new LdapName(principalNames);
				// TODO differentiate groups and users ?
				Principal principal = getOrCreatePrincipal(session, principalNames);
				JcrUtils.addPrivileges(session, path, principal, privs);
			} catch (InvalidNameException e) {
				for (String principalName : principalNames.split(",")) {
					Principal principal = getOrCreatePrincipal(session, principalName);
					JcrUtils.addPrivileges(session, path, principal, privs);
					// if (log.isDebugEnabled()) {
					// StringBuffer privBuf = new StringBuffer();
					// for (Privilege priv : privs)
					// privBuf.append(priv.getName());
					// log.debug("Added privileges " + privBuf + " to "
					// + principal.getName() + " on " + path + " in '"
					// + session.getWorkspace().getName() + "'");
					// }
				}
			}
		}

		// if (log.isDebugEnabled())
		// log.debug("JCR authorizations applied on '"
		// + session.getWorkspace().getName() + "'");
	}

	/**
	 * Returns a {@link SimplePrincipal}, does not check whether it exists since
	 * such capabilities is not provided by the standard JCR API. Can be
	 * overridden to provide smarter handling
	 */
	protected Principal getOrCreatePrincipal(Session session, String principalName) throws RepositoryException {
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
	// throw new ArgeoJcrException("Don't know how to apply privileges "
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

	public void setSecurityWorkspace(String securityWorkspace) {
		this.securityWorkspace = securityWorkspace;
	}

}
