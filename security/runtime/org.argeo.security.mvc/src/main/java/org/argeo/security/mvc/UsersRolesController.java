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

package org.argeo.security.mvc;

import java.io.Reader;
import java.util.List;

import org.argeo.security.ArgeoSecurityService;
import org.argeo.security.ArgeoUser;
import org.argeo.security.SimpleArgeoUser;
import org.argeo.server.BooleanAnswer;
import org.argeo.server.Deserializer;
import org.argeo.server.ServerAnswer;
import org.argeo.server.mvc.MvcConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsersRolesController implements MvcConstants {
	// private final static Log log = LogFactory
	// .getLog(UsersRolesController.class);

	private ArgeoSecurityService securityService;

	private Deserializer userDeserializer = null;

	/* USER */

	@RequestMapping("/getCredentials.ria")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser getCredentials() {
		ArgeoUser argeoUser = securityService.getSecurityDao().getCurrentUser();
		if (argeoUser == null)
			return new SimpleArgeoUser();
		else
			return argeoUser;
	}

	@RequestMapping("/getUsersList.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public List<ArgeoUser> getUsersList() {
		return securityService.getSecurityDao().listUsers();
	}

	@RequestMapping("/userExists.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public BooleanAnswer userExists(@RequestParam("username") String username) {
		return new BooleanAnswer(securityService.getSecurityDao().userExists(
				username));
	}

	@RequestMapping("/createUser.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser createUser(Reader reader) {
		ArgeoUser user = userDeserializer.deserialize(reader,
				SimpleArgeoUser.class);
		// cleanUserBeforeCreate(user);
		securityService.newUser(user);
		return securityService.getSecurityDao().getUser(user.getUsername());
	}

	@RequestMapping("/updateUser.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser updateUser(Reader reader) {
		ArgeoUser user = userDeserializer.deserialize(reader,
				SimpleArgeoUser.class);
		securityService.updateUser(user);
		return securityService.getSecurityDao().getUser(user.getUsername());
	}

	@RequestMapping("/updateUserSelf.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	/** Will only update the user natures.*/
	public ArgeoUser updateUserSelf(Reader reader) {
		ArgeoUser user = securityService.getSecurityDao().getCurrentUser();
		ArgeoUser userForNatures = userDeserializer.deserialize(reader,
				SimpleArgeoUser.class);
		user.updateUserNatures(userForNatures.getUserNatures());
		securityService.updateUser(user);
		return securityService.getSecurityDao().getUser(user.getUsername());
	}

	@RequestMapping("/deleteUser.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer deleteUser(@RequestParam("username") String username) {
		securityService.getSecurityDao().delete(username);
		return ServerAnswer.ok("User " + username + " deleted");
	}

	@RequestMapping("/getUserDetails.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser getUserDetails(@RequestParam("username") String username) {
		return securityService.getSecurityDao().getUser(username);
	}

	/* ROLE */
	@RequestMapping("/getRolesList.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public List<String> getEditableRolesList() {
		return securityService.getSecurityDao().listEditableRoles();
	}

	@RequestMapping("/createRole.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer createRole(@RequestParam("role") String role) {
		securityService.newRole(role);
		return ServerAnswer.ok("Role " + role + " created");
	}

	@RequestMapping("/deleteRole.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer deleteRole(@RequestParam("role") String role) {
		securityService.getSecurityDao().deleteRole(role);
		return ServerAnswer.ok("Role " + role + " deleted");
	}

	@RequestMapping("/updateUserPassword.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer updateUserPassword(
			@RequestParam("username") String username,
			@RequestParam("password") String password) {
		securityService.updateUserPassword(username, password);
		return ServerAnswer.ok("Password updated for user " + username);
	}

	@RequestMapping("/updatePassword.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer updatePassword(
			@RequestParam("oldPassword") String oldPassword,
			@RequestParam("password") String password) {
		securityService.updateCurrentUserPassword(oldPassword, password);
		return ServerAnswer.ok("Password updated");
	}

	public void setUserDeserializer(Deserializer userDeserializer) {
		this.userDeserializer = userDeserializer;
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
