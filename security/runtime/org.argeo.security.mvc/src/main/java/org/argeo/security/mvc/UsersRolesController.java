package org.argeo.security.mvc;

import java.util.List;

import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.argeo.security.dao.RoleDao;
import org.argeo.security.dao.UserDao;
import org.argeo.server.BooleanAnswer;
import org.argeo.server.ServerAnswer;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsersRolesController {
	private UserDao userDao;
	private RoleDao roleDao;

	@RequestMapping("/getCredentials.security")
	@ModelAttribute("getCredentials")
	public ArgeoUser getCredentials() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();

		return ArgeoUserDetails.asArgeoUser(authentication);
	}

	@RequestMapping("/getUsersList.security")
	@ModelAttribute("getUsersList")
	public List<ArgeoUser> getUsersList() {
		return userDao.listUsers();
	}

	@RequestMapping("/userExists.security")
	@ModelAttribute("userExists")
	public BooleanAnswer userExists(@RequestParam("username") String username) {
		return new BooleanAnswer(userDao.userExists(username));
	}

	@RequestMapping("/deleteUser.security")
	@ModelAttribute("deleteUser")
	public ServerAnswer deleteUser(@RequestParam("username") String username) {
		userDao.delete(username);
		return ServerAnswer.ok(username + " deleted");
	}

	@RequestMapping("/getUserDetails.security")
	@ModelAttribute("getUserDetails")
	public ArgeoUser getUserDetails(@RequestParam("username") String username) {
		return userDao.getUser(username);
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setRoleDao(RoleDao roleDao) {
		this.roleDao = roleDao;
	}

}
