/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

package org.argeo.security.core;

import java.util.Map;

import org.argeo.security.ArgeoUser;
import org.argeo.security.CurrentUserDao;
import org.argeo.security.CurrentUserService;
import org.argeo.security.UserNature;

@Deprecated
public class DefaultCurrentUserService implements CurrentUserService {
	private CurrentUserDao currentUserDao;

	public DefaultCurrentUserService() {
	}

	public ArgeoUser getCurrentUser() {
		ArgeoUser argeoUser = ArgeoUserDetails.securityContextUser();
		if (argeoUser == null)
			return null;
		if (argeoUser.getRoles().contains(currentUserDao.getDefaultRole()))
			argeoUser.getRoles().remove(currentUserDao.getDefaultRole());
		return argeoUser;
	}

	public void updateCurrentUserPassword(String oldPassword, String newPassword) {
		currentUserDao.updateCurrentUserPassword(oldPassword, newPassword);
	}

	public void updateCurrentUserNatures(Map<String, UserNature> userNatures) {
		// TODO Auto-generated method stub

	}

	public void setCurrentUserDao(CurrentUserDao dao) {
		this.currentUserDao = dao;
	}
}
