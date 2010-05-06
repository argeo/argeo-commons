package org.argeo.security.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.security.AuthenticationException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

public class MatchingAuthenticationProvider extends
		AbstractUserDetailsAuthenticationProvider {

	private Resource mapping;
	private Properties properties;

	private List<String> defaultRoles = new ArrayList<String>();

	@Override
	protected void doAfterPropertiesSet() throws Exception {
		properties = new Properties();
		InputStream propIn = mapping.getInputStream();
		try {
			properties.load(propIn);
		} finally {
			propIn.close();
		}
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		if (!userDetails.getPassword().equals(authentication.getCredentials()))
			throw new BadCredentialsException(
					"Invalid credentails provided by "
							+ authentication.getName());
	}

	@Override
	protected UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		String value = properties.getProperty(username);
		if (value == null)
			throw new BadCredentialsException("User " + username
					+ " is not registered");
		List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
		for (String role : defaultRoles)
			grantedAuthorities.add(new GrantedAuthorityImpl(role));
		return new User(
				username,
				value,
				true,
				true,
				true,
				true,
				grantedAuthorities
						.toArray(new GrantedAuthority[grantedAuthorities.size()]));
	}

	public void setMapping(Resource mapping) {
		this.mapping = mapping;
	}

	public void setDefaultRoles(List<String> defaultRoles) {
		this.defaultRoles = defaultRoles;
	}

}
