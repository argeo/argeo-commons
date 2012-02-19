package org.argeo.security.jcr.rememberme;

import java.util.Date;

import org.springframework.security.ui.rememberme.PersistentRememberMeToken;
import org.springframework.security.ui.rememberme.PersistentTokenRepository;

public class JcrPersistentTokenRepository implements PersistentTokenRepository {

	public void createNewToken(PersistentRememberMeToken token) {
		// TODO Auto-generated method stub

	}

	public void updateToken(String series, String tokenValue, Date lastUsed) {
		// TODO Auto-generated method stub

	}

	public PersistentRememberMeToken getTokenForSeries(String seriesId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeUserTokens(String username) {
		// TODO Auto-generated method stub

	}

}
