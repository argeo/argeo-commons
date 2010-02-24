package org.argeo.security.core;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurity;
import org.argeo.security.ArgeoSecurityDao;
import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;

public class DefaultSecurityService implements ArgeoSecurityService {
	private ArgeoSecurity argeoSecurity = new DefaultArgeoSecurity();
	private ArgeoSecurityDao securityDao;

	public ArgeoSecurityDao getSecurityDao() {
		return securityDao;
	}

	public void newRole(String role) {
		securityDao.createRole(role, argeoSecurity.getSuperUsername());
	}

	public void updateUserPassword(String username, String password) {
		SimpleArgeoUser user = new SimpleArgeoUser(securityDao
				.getUser(username));
		user.setPassword(password);
		securityDao.update(user);
	}

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		SimpleArgeoUser user = new SimpleArgeoUser(securityDao.getCurrentUser());
		if (!user.getPassword().equals(oldPassword))
			throw new ArgeoException("Old password is not correct.");
		user.setPassword(newPassword);
		securityDao.update(user);
	}

	public void newUser(ArgeoUser user) {
		user.getUserNatures().clear();
		argeoSecurity.beforeCreate(user);
		securityDao.create(user);
	}

	public void updateUser(ArgeoUser user) {
		String password = securityDao.getUserWithPassword(user.getUsername())
				.getPassword();
		SimpleArgeoUser simpleArgeoUser = new SimpleArgeoUser(user);
		simpleArgeoUser.setPassword(password);
		securityDao.update(simpleArgeoUser);
	}

	public void setArgeoSecurity(ArgeoSecurity argeoSecurity) {
		this.argeoSecurity = argeoSecurity;
	}

	public void setSecurityDao(ArgeoSecurityDao dao) {
		this.securityDao = dao;
	}

}
