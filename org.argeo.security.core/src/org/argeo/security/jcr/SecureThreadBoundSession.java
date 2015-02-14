/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.security.jcr;

import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.spring.ThreadBoundSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Thread bounded JCR session factory which checks authentication and is
 * autoconfigured in Spring.
 */
@Deprecated
public class SecureThreadBoundSession extends ThreadBoundSession {
	private final static Log log = LogFactory
			.getLog(SecureThreadBoundSession.class);

	@Override
	protected Session preCall(Session session) {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null) {
			String userID = session.getUserID();
			String currentUserName = authentication.getName();
			if (currentUserName != null) {
				if (!userID.equals(currentUserName)) {
					log.warn("Current session has user ID " + userID
							+ " while logged is user is " + currentUserName
							+ "(authentication=" + authentication + ")"
							+ ". Re-login.");
					// TODO throw an exception
					return login();
				}
			}
		}
		return super.preCall(session);
	}

}
