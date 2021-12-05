package org.argeo.jackrabbit.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.argeo.jcr.JcrUtils;

/** Utilities around Jackrabbit security extensions. */
public class JackrabbitSecurityUtils {
	private final static Log log = LogFactory.getLog(JackrabbitSecurityUtils.class);

	/**
	 * Convenience method for denying a single privilege to a principal (user or
	 * role), typically jcr:all
	 */
	public synchronized static void denyPrivilege(Session session, String path, String principal, String privilege)
			throws RepositoryException {
		List<Privilege> privileges = new ArrayList<Privilege>();
		privileges.add(session.getAccessControlManager().privilegeFromName(privilege));
		denyPrivileges(session, path, () -> principal, privileges);
	}

	/**
	 * Deny privileges on a path to a {@link Principal}. The path must already
	 * exist. Session is saved. Synchronized to prevent concurrent modifications of
	 * the same node.
	 */
	public synchronized static Boolean denyPrivileges(Session session, String path, Principal principal,
			List<Privilege> privs) throws RepositoryException {
		// make sure the session is in line with the persisted state
		session.refresh(false);
		JackrabbitAccessControlManager acm = (JackrabbitAccessControlManager) session.getAccessControlManager();
		JackrabbitAccessControlList acl = (JackrabbitAccessControlList) JcrUtils.getAccessControlList(acm, path);

//		accessControlEntries: for (AccessControlEntry ace : acl.getAccessControlEntries()) {
//			Principal currentPrincipal = ace.getPrincipal();
//			if (currentPrincipal.getName().equals(principal.getName())) {
//				Privilege[] currentPrivileges = ace.getPrivileges();
//				if (currentPrivileges.length != privs.size())
//					break accessControlEntries;
//				for (int i = 0; i < currentPrivileges.length; i++) {
//					Privilege currP = currentPrivileges[i];
//					Privilege p = privs.get(i);
//					if (!currP.getName().equals(p.getName())) {
//						break accessControlEntries;
//					}
//				}
//				return false;
//			}
//		}

		Privilege[] privileges = privs.toArray(new Privilege[privs.size()]);
		acl.addEntry(principal, privileges, false);
		acm.setPolicy(path, acl);
		if (log.isDebugEnabled()) {
			StringBuffer privBuf = new StringBuffer();
			for (Privilege priv : privs)
				privBuf.append(priv.getName());
			log.debug("Denied privileges " + privBuf + " to " + principal.getName() + " on " + path + " in '"
					+ session.getWorkspace().getName() + "'");
		}
		session.refresh(true);
		session.save();
		return true;
	}

	/** Singleton. */
	private JackrabbitSecurityUtils() {

	}
}
