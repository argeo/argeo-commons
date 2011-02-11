package org.argeo.security.nature;

import org.argeo.security.AbstractUserNature;

/** User with access to POSIX operating systems*/
public class PosixAccountNature extends AbstractUserNature {
	private static final long serialVersionUID = 1L;

	private Integer uidNumber;
	private Integer gidNumber;
	private String homeDirectory;
	private String authorizedKeys;

	public Integer getUidNumber() {
		return uidNumber;
	}

	public void setUidNumber(Integer uidNumber) {
		this.uidNumber = uidNumber;
	}

	public Integer getGidNumber() {
		return gidNumber;
	}

	public void setGidNumber(Integer gidNumber) {
		this.gidNumber = gidNumber;
	}

	public String getHomeDirectory() {
		return homeDirectory;
	}

	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	public String getAuthorizedKeys() {
		return authorizedKeys;
	}

	public void setAuthorizedKeys(String authorizedKeys) {
		this.authorizedKeys = authorizedKeys;
	}
}
