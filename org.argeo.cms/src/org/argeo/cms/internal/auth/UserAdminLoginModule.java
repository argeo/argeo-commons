package org.argeo.cms.internal.auth;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.CredentialNotFoundException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.cms.CmsException;
import org.argeo.cms.KernelHeader;
import org.argeo.cms.internal.kernel.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

public class UserAdminLoginModule implements LoginModule {
	private Subject subject;
	private CallbackHandler callbackHandler;
	private boolean isAnonymous = false;

	private final static LdapName ROLE_ADMIN_NAME, ROLE_USER_NAME,
			ROLE_ANONYMOUS_NAME;
	private final static List<LdapName> RESERVED_ROLES;
	private final static X500Principal ROLE_ANONYMOUS_PRINCIPAL;
	static {
		try {
			ROLE_ADMIN_NAME = new LdapName(KernelHeader.ROLE_ADMIN);
			ROLE_USER_NAME = new LdapName(KernelHeader.ROLE_USER);
			ROLE_ANONYMOUS_NAME = new LdapName(KernelHeader.ROLE_ANONYMOUS);
			RESERVED_ROLES = Collections.unmodifiableList(Arrays
					.asList(new LdapName[] { ROLE_ANONYMOUS_NAME,
							ROLE_USER_NAME, ROLE_ADMIN_NAME,
							new LdapName(KernelHeader.ROLE_GROUP_ADMIN),
							new LdapName(KernelHeader.ROLE_USER_ADMIN) }));
			ROLE_ANONYMOUS_PRINCIPAL = new X500Principal(
					ROLE_ANONYMOUS_NAME.toString());
		} catch (InvalidNameException e) {
			throw new Error("Cannot initialize login module class", e);
		}
	}

	private Authorization authorization;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {
		try {
			this.subject = subject;
			this.callbackHandler = callbackHandler;
			if (options.containsKey("anonymous"))
				isAnonymous = Boolean.parseBoolean(options.get("anonymous")
						.toString());
			// String ldifFile = options.get("ldifFile").toString();
			// InputStream in = new URL(ldifFile).openStream();
			// userAdmin = new LdifUserAdmin(in);
		} catch (Exception e) {
			throw new CmsException("Cannot initialize login module", e);
		}
	}

	@Override
	public boolean login() throws LoginException {
		// TODO use a callback in order to get the bundle context
		BundleContext bc = Activator.getBundleContext();
		UserAdmin userAdmin = bc.getService(bc
				.getServiceReference(UserAdmin.class));
		final User user;

		if (!isAnonymous) {
			// ask for username and password
			NameCallback nameCallback = new NameCallback("User");
			PasswordCallback passwordCallback = new PasswordCallback(
					"Password", false);
			// handle callbacks
			try {
				callbackHandler.handle(new Callback[] { nameCallback,
						passwordCallback });
			} catch (Exception e) {
				throw new CmsException("Cannot handle callbacks", e);
			}

			// create credentials
			final String username = nameCallback.getName();
			if (username == null || username.trim().equals(""))
				throw new CredentialNotFoundException("No credentials provided");

			char[] password = {};
			if (passwordCallback.getPassword() != null)
				password = passwordCallback.getPassword();
			else
				throw new CredentialNotFoundException("No credentials provided");

			// user = (User) userAdmin.getRole(username);
			user = userAdmin.getUser(null, username);
			if (user == null)
				return false;

			byte[] hashedPassword = ("{SHA}" + Base64
					.encodeBase64String(DigestUtils.sha1(toBytes(password))))
					.getBytes();
			if (!user.hasCredential("userpassword", hashedPassword))
				return false;
		} else
			// anonymous
			user = null;
		this.authorization = userAdmin.getAuthorization(user);
		return true;
	}

	private byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
				byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	@Override
	public boolean commit() throws LoginException {
		if (authorization != null) {
			Set<Principal> principals = subject.getPrincipals();
			try {
				String authName = authorization.getName();

				// determine user's principal
				final LdapName name;
				final Principal userPrincipal;
				if (authName == null) {
					name = ROLE_ANONYMOUS_NAME;
					userPrincipal = ROLE_ANONYMOUS_PRINCIPAL;
					principals.add(userPrincipal);
					principals.add(new AnonymousPrincipal());
				} else {
					name = new LdapName(authName);
					checkUserName(name);
					userPrincipal = new X500Principal(name.toString());
					principals.add(userPrincipal);
					principals.add(new ImpliedByPrincipal(ROLE_USER_NAME,
							userPrincipal));
				}

				// Add roles provided by authorization
				for (String role : authorization.getRoles()) {
					LdapName roleName = new LdapName(role);
					if (roleName.equals(name)) {
						// skip
					} else {
						checkImpliedPrincipalName(roleName);
						principals.add(new ImpliedByPrincipal(roleName
								.toString(), userPrincipal));
						if (roleName.equals(ROLE_ADMIN_NAME))
							principals.add(new AdminPrincipal(
									SecurityConstants.ADMIN_ID));
					}
				}

				return true;
			} catch (InvalidNameException e) {
				throw new CmsException("Cannot commit", e);
			}
		} else
			return false;
	}

	@Override
	public boolean abort() throws LoginException {
		cleanUp();
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		// TODO better deal with successive logout
		if (subject == null)
			return true;
		// TODO make it less brutal
		subject.getPrincipals().removeAll(
				subject.getPrincipals(X500Principal.class));
		subject.getPrincipals().removeAll(
				subject.getPrincipals(ImpliedByPrincipal.class));
		cleanUp();
		return true;
	}

	private void cleanUp() {
		subject = null;
		authorization = null;
	}

	private void checkUserName(LdapName name) {
		if (RESERVED_ROLES.contains(name))
			throw new CmsException(name + " is a reserved name");
	}

	private void checkImpliedPrincipalName(LdapName roleName) {
		if (ROLE_USER_NAME.equals(roleName)
				|| ROLE_ANONYMOUS_NAME.equals(roleName))
			throw new CmsException(roleName + " cannot be listed as role");
	}
}
