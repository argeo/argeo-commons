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

	// @RequestMapping("/login.security")
	// @ModelAttribute(ANSWER_MODEL_KEY)
	// public ArgeoUser login(@RequestParam("username") String username,
	// @RequestParam("password") String password) {
	// //SecurityContextHolder.getContext().getAuthentication().
	// return securityService.getSecurityDao().getCurrentUser();
	// }
	//
	// @RequestMapping("/logout.security")
	// @ModelAttribute(ANSWER_MODEL_KEY)
	// public ServerAnswer logout() {
	// return ServerAnswer.ok("Logged out");
	// }

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
		ArgeoUser user = userDeserializer.deserialize(reader, ArgeoUser.class);
		// cleanUserBeforeCreate(user);
		securityService.newUser(user);
		return securityService.getSecurityDao().getUser(user.getUsername());
	}

	@RequestMapping("/updateUser.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser updateUser(Reader reader) {
		ArgeoUser user = userDeserializer.deserialize(reader, ArgeoUser.class);
		securityService.updateUser(user);
		return securityService.getSecurityDao().getUser(user.getUsername());
	}

	/*
	 * @RequestMapping("/createUser2.security")
	 * 
	 * @ModelAttribute(ANSWER_MODEL_KEY) public ArgeoUser
	 * createUser(@RequestParam("body") String body) { if (log.isDebugEnabled())
	 * log.debug("body:\n" + body); StringReader reader = new
	 * StringReader(body); ArgeoUser user = null; try { user = (ArgeoUser)
	 * userDeserializer.deserialize(reader); } finally {
	 * IOUtils.closeQuietly(reader); } cleanUserBeforeCreate(user);
	 * securityService.newUser(user); return
	 * securityService.getSecurityDao().getUser(user.getUsername()); }
	 */

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
			@RequestParam("password") String password,
			@RequestParam("oldPassword") String oldPassword) {
		securityService.getSecurityDao().updatePassword(oldPassword, password);
		return ServerAnswer.ok("Password updated");
	}

	// protected void cleanUserBeforeCreate(ArgeoUser user) {
	// user.getUserNatures().clear();
	// }

	public void setUserDeserializer(Deserializer userDeserializer) {
		this.userDeserializer = userDeserializer;
	}

	public void setSecurityService(ArgeoSecurityService securityService) {
		this.securityService = securityService;
	}

}
