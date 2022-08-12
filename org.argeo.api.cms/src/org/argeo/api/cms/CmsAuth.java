package org.argeo.api.cms;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** The type of login context to use. */
public enum CmsAuth {
	NODE, USER, ANONYMOUS, DATA_ADMIN, SINGLE_USER, KEYRING;

	public String getLoginContextName() {
		return name();
	}

	@Override
	public String toString() {
		return getLoginContextName();
	}

	public LoginContext newLoginContext(CallbackHandler callbackHandler) throws LoginException {
		return new LoginContext(getLoginContextName(), callbackHandler);
	}

	public LoginContext newLoginContext(Subject subject, CallbackHandler callbackHandler) throws LoginException {
		return new LoginContext(getLoginContextName(), subject, callbackHandler);
	}

	public LoginContext newLoginContext(Subject subject) throws LoginException {
		return new LoginContext(getLoginContextName(), subject);
	}

	public LoginContext newLoginContext() throws LoginException {
		return new LoginContext(getLoginContextName());
	}

	/*
	 * LOGIN CONTEXTS
	 */
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_NODE = NODE.getLoginContextName();
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_USER = USER.getLoginContextName();
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_ANONYMOUS = ANONYMOUS.getLoginContextName();
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_DATA_ADMIN = DATA_ADMIN.getLoginContextName();
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_SINGLE_USER = SINGLE_USER.getLoginContextName();
	/** @deprecated Use enum instead. */
	@Deprecated
	public static final String LOGIN_CONTEXT_KEYRING = KEYRING.getLoginContextName();

}
