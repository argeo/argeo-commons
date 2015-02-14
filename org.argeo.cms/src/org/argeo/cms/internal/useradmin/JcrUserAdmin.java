package org.argeo.cms.internal.useradmin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.useradmin.jackrabbit.JackrabbitUserAdminService;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.UserAdminService;
import org.argeo.security.jcr.JcrUserDetails;
import org.argeo.security.jcr.NewUserDetails;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Incomplete implementation of {@link UserAdmin} wrapping the supported
 * {@link UserAdminService} for the time being.
 */
public class JcrUserAdmin implements UserAdmin {
	private final BundleContext bundleContext;
	private JackrabbitUserAdminService userAdminService;

	private final Session session;

	public JcrUserAdmin(BundleContext bundleContext, Repository node) {
		try {
			this.bundleContext = bundleContext;
			this.session = node.login();
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize user admin", e);
		}
	}

	public void destroy() {
		JcrUtils.logoutQuietly(session);
	}

	@Override
	public Role createRole(String name, int type) {
		if (Role.USER == type) {
			NewUserDetails userDetails = new NewUserDetails(name, null);
			userAdminService().createUser(userDetails);
			return new JcrEndUser((JcrUserDetails) userAdminService()
					.loadUserByUsername(name));
		} else if (Role.GROUP == type) {
			userAdminService().newRole(name);
			return new JcrGroup(name);
		} else {
			throw new ArgeoException("Unsupported role type " + type);
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
		try {
			JcrUserDetails userDetails = (JcrUserDetails) userAdminService()
					.loadUserByUsername(name);
			return new JcrEndUser(userDetails);
		} catch (UsernameNotFoundException e) {
			if (userAdminService().listEditableRoles().contains(name))
				return new JcrGroup(name);
			else
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
			res[roles.size() + i] = new JcrEndUser(
					(JcrUserDetails) userAdminService().loadUserByUsername(
							users.get(i)));
		return res;
	}

	@Override
	public User getUser(String key, String value) {
		throw new CmsException("Property based search not yet implemented");
	}

	@Override
	public Authorization getAuthorization(User user) {
		return new JcrAuthorization(((JcrEndUser) user).getUserDetails());
	}

	private synchronized UserAdminService userAdminService() {
		return userAdminService;
	}

	public void setUserAdminService(JackrabbitUserAdminService userAdminService) {
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
