package org.argeo.security.mvc;

import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.argeo.security.dao.RoleDao;
import org.argeo.security.dao.UserDao;
import org.argeo.server.BooleanAnswer;
import org.argeo.server.ServerAnswer;
import org.argeo.server.mvc.MvcConstants;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsersRolesController implements MvcConstants {
	private UserDao userDao;
	private RoleDao roleDao;

	/* USER */

	@RequestMapping("/getCredentials.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser getCredentials() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		return ArgeoUserDetails.asArgeoUser(authentication);
	}

	@RequestMapping("/getUsersList.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public List<ArgeoUser> getUsersList() {
		return userDao.listUsers();
	}

	@RequestMapping("/userExists.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public BooleanAnswer userExists(@RequestParam("username") String username) {
		return new BooleanAnswer(userDao.userExists(username));
	}

	@RequestMapping("/deleteUser.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer deleteUser(@RequestParam("username") String username) {
		userDao.delete(username);
		return ServerAnswer.ok("User " + username + " deleted");
	}

	@RequestMapping("/getUserDetails.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ArgeoUser getUserDetails(@RequestParam("username") String username) {
		return userDao.getUser(username);
	}

	/* ROLE */
	@RequestMapping("/getRolesList.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public List<String> getEditableRolesList() {
		return roleDao.listEditableRoles();
	}

	@RequestMapping("/createRole.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer createRole(@RequestParam("role") String role) {
		roleDao.create(role);
		return ServerAnswer.ok("Role " + role + " created");
	}

	@RequestMapping("/deleteRole.security")
	@ModelAttribute(ANSWER_MODEL_KEY)
	public ServerAnswer deleteRole(@RequestParam("role") String role) {
		roleDao.delete(role);
		return ServerAnswer.ok("Role " + role + " created");
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setRoleDao(RoleDao roleDao) {
		this.roleDao = roleDao;
	}

}
