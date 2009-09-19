package org.argeo.security;

/** Callback related to the lifecycle of a user. */
public interface ArgeoSecurity {
	/**
	 * Called before a user is actually created. Default user natures and roles
	 * should be added there.
	 */
	public void beforeCreate(ArgeoUser user);

	public String getSuperUsername();
}
