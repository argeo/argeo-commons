package org.argeo.security.core;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class InternalAuthenticationProvider implements AuthenticationProvider {
	private String key;

	public InternalAuthenticationProvider() {
	}

	public InternalAuthenticationProvider(String key) {
		this.key = key;
	}

	@Override
	public Authentication authenticate(Authentication arg0)
			throws AuthenticationException {
		InternalAuthentication authentication = (InternalAuthentication) arg0;
		if (authentication.getCredentials().toString().equals(key))
			return authentication;
		return null;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return InternalAuthentication.class.isAssignableFrom(authentication);
	}

}
