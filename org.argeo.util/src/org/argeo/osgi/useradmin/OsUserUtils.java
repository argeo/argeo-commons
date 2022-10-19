package org.argeo.osgi.useradmin;

import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.URIParameter;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** Log in based on JDK-provided OS integration. */
public class OsUserUtils {
	private final static String LOGIN_CONTEXT_USER_NIX = "USER_NIX";
	private final static String LOGIN_CONTEXT_USER_NT = "USER_NT";

	public static String getOsUsername() {
		return System.getProperty("user.name");
	}

	public static LoginContext loginAsSystemUser(Subject subject) {
		try {
			URL jaasConfigurationUrl = OsUserUtils.class.getClassLoader()
					.getResource("org/argeo/osgi/useradmin/jaas-os.cfg");
			URIParameter uriParameter = new URIParameter(jaasConfigurationUrl.toURI());
			Configuration jaasConfiguration = Configuration.getInstance("JavaLoginConfig", uriParameter);
			LoginContext lc = new LoginContext(isWindows() ? LOGIN_CONTEXT_USER_NT : LOGIN_CONTEXT_USER_NIX, subject,
					null, jaasConfiguration);
			lc.login();
			return lc;
		} catch (URISyntaxException | NoSuchAlgorithmException | LoginException e) {
			throw new RuntimeException("Cannot login as system user", e);
		}
	}

	public static void main(String args[]) {
		Subject subject = new Subject();
		LoginContext loginContext = loginAsSystemUser(subject);
		System.out.println(subject);
		try {
			loginContext.logout();
		} catch (LoginException e) {
			// silent
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}

	private OsUserUtils() {
	}
}
