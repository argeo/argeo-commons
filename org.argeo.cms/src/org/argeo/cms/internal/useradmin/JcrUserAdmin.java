package org.argeo.cms.internal.useradmin;

import static org.argeo.jcr.ArgeoJcrConstants.ALIAS_NODE;
import static org.argeo.jcr.ArgeoJcrConstants.JCR_REPOSITORY_ALIAS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrSecurityModel;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.jcr.SimpleJcrSecurityModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class JcrUserAdmin implements UserAdmin {
	private final BundleContext bundleContext;
	private UserAdminService userAdminService;

	private final JcrSecurityModel jcrSecurityModel = new SimpleJcrSecurityModel();
	private final Session session;

	public JcrUserAdmin(BundleContext bundleContext) {
		try {
			this.bundleContext = bundleContext;

			ServiceReference<Repository> nodeRepo = bundleContext
					.getServiceReferences(Repository.class,
							"(" + JCR_REPOSITORY_ALIAS + "=" + ALIAS_NODE + ")")
					.iterator().next();
			this.session = bundleContext.getService(nodeRepo).login();
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize user admin", e);
		}
	}

	@Override
	public Role createRole(String name, int type) {
		try {
			if (Role.USER == type) {
				Node userProfile = jcrSecurityModel.sync(session, name, null);
				session.getWorkspace().getVersionManager()
						.checkout(userProfile.getPath());
				String password = "";
				// TODO add roles
				JcrUserDetails userDetails = new JcrUserDetails(userProfile,
						password, new ArrayList<GrantedAuthority>());
				session.save();
				session.getWorkspace().getVersionManager()
						.checkin(userProfile.getPath());
				userAdminService().createUser(userDetails);
				return new JcrEndUser(userDetails);
			} else if (Role.GROUP == type) {
				userAdminService().newRole(name);
				return new JcrGroup(name);
			} else {
				throw new ArgeoException("Unsupported role type " + type);
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create role " + name);
		}
	}

	@Override
	public boolean removeRole(String name) {
		Role role = getRole(name);
		if (role == null)
			return false;
		if (role instanceof JcrEndUser)
			userAdminService().deleteUser(role.getName());
		else if (role instanceof JcrGroup)
			userAdminService().deleteRole(role.getName());
		else
			return false;
		return true;
	}

	@Override
	public Role getRole(String name) {
		if (userAdminService().listEditableRoles().contains(name))
			return new JcrGroup(name);
		try {
			UserDetails userDetails = userAdminService().loadUserByUsername(
					name);
			return new JcrEndUser(userDetails);
		} catch (UsernameNotFoundException e) {
			return null;
		}
	}

	@Override
	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		if (filter != null)
			throw new ArgeoException("Filtering not yet implemented");
		List<String> roles = new ArrayList<String>(userAdminService()
				.listEditableRoles());
		List<String> users = new ArrayList<String>(userAdminService()
				.listUsers());
		Role[] res = new Role[users.size() + roles.size()];
		for (int i = 0; i < roles.size(); i++)
			res[i] = new JcrGroup(roles.get(i));
		for (int i = 0; i < users.size(); i++)
			res[roles.size() + i] = new JcrEndUser(userAdminService()
					.loadUserByUsername(users.get(i)));
		return res;
	}

	@Override
	public User getUser(String key, String value) {
		throw new ArgeoException("Property based search not yet implemented");
	}

	@Override
	public Authorization getAuthorization(User user) {
		return null;
	}

	private synchronized UserAdminService userAdminService() {
		return userAdminService;
	}

	public void setUserAdminService(UserAdminService userAdminService) {
		this.userAdminService = userAdminService;
	}

	protected synchronized void notifyEvent(UserAdminEvent event) {
		try {
			Collection<ServiceReference<UserAdminListener>> sr = bundleContext
					.getServiceReferences(UserAdminListener.class, null);
			for (Iterator<ServiceReference<UserAdminListener>> it = sr
					.iterator(); it.hasNext();) {
				UserAdminListener listener = bundleContext
						.getService(it.next());
				listener.roleChanged(event);
			}
		} catch (InvalidSyntaxException e) {
			throw new ArgeoException("Cannot notify listeners", e);
		}
	}
}
