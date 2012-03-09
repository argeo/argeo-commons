/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
