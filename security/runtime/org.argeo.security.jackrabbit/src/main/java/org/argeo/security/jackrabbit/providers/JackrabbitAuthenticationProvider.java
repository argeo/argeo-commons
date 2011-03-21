package org.argeo.security.jackrabbit.providers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.argeo.ArgeoException;
import org.argeo.security.jcr.JcrAuthenticationProvider;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

public class JackrabbitAuthenticationProvider extends JcrAuthenticationProvider {

	@Override
	protected GrantedAuthority[] getGrantedAuthorities(Session session) {
		try {
			JackrabbitSession jackrabbitSession = (JackrabbitSession) session;
			UserManager userManager = jackrabbitSession.getUserManager();
			User user = (User) userManager.getAuthorizable(session.getUserID());
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			for (Iterator<Group> it = user.memberOf(); it.hasNext();)
				authorities.add(new GrantedAuthorityImpl(it.next().getID()));
			return authorities
					.toArray(new GrantedAuthority[authorities.size()]);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve authorities for "
					+ session.getUserID(), e);
		}
	}

	@Override
	protected Boolean isEnabled(Node userHome) {
		try {
			UserManager userManager = ((JackrabbitSession) userHome
					.getSession()).getUserManager();
			User user = (User) userManager.getAuthorizable(userHome
					.getSession().getUserID());
			return !user.isDisabled();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check whether " + userHome
					+ " is enabled", e);
		}
	}

}
