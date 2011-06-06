package org.argeo.jackrabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.jcr.JcrUtils;

/** Apply authorizations to a Jackrabbit repository. */
public class JackrabbitAuthorizations {
	private final static Log log = LogFactory
			.getLog(JackrabbitAuthorizations.class);

	private Repository repository;
	private Executor systemExecutor;

	/**
	 * key := privilege1,privilege2/path/to/node<br/>
	 * value := group1,group2
	 */
	private Map<String, String> groupPrivileges = new HashMap<String, String>();

	public void init() {
		Runnable action = new Runnable() {
			public void run() {
				JackrabbitSession session = null;
				try {
					session = (JackrabbitSession) repository.login();
					initAuthorizations(session);
				} catch (Exception e) {
					JcrUtils.discardQuietly(session);
				} finally {
					JcrUtils.logoutQuietly(session);
				}
			}
		};

		if (systemExecutor != null)
			systemExecutor.execute(action);
		else
			action.run();
	}

	protected void initAuthorizations(JackrabbitSession session)
			throws RepositoryException {
		JackrabbitAccessControlManager acm = (JackrabbitAccessControlManager) session
				.getAccessControlManager();
		UserManager um = session.getUserManager();

		for (String privileges : groupPrivileges.keySet()) {
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

			String groupNames = groupPrivileges.get(privileges);
			for (String groupName : groupNames.split(",")) {
				Group group = (Group) um.getAuthorizable(groupName);
				if (group == null)
					group = um.createGroup(groupName);

				AccessControlPolicy policy = null;
				AccessControlPolicyIterator policyIterator = acm
						.getApplicablePolicies(path);
				if (policyIterator.hasNext()) {
					policy = policyIterator.nextAccessControlPolicy();
				} else {
					AccessControlPolicy[] existingPolicies = acm
							.getPolicies(path);
					policy = existingPolicies[0];
				}
				if (policy instanceof AccessControlList) {
					((AccessControlList) policy).addAccessControlEntry(
							group.getPrincipal(),
							privs.toArray(new Privilege[privs.size()]));
					acm.setPolicy(path, policy);
				}
				if (log.isDebugEnabled())
					log.debug("Added privileges " + privileges + " to "
							+ groupName + " on " + path);
			}
		}
		session.save();
	}

	public void setGroupPrivileges(Map<String, String> groupPrivileges) {
		this.groupPrivileges = groupPrivileges;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSystemExecutor(Executor systemExecutor) {
		this.systemExecutor = systemExecutor;
	}

}
