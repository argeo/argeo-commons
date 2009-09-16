package org.argeo.security.mvc;

import org.argeo.security.ArgeoUser;
import org.argeo.security.core.ArgeoUserDetails;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UsersRolesController {

	@RequestMapping("/getCredentials.security")
	@ModelAttribute("getCredentials")
	public ArgeoUser getCredentials() {
		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();

		return ArgeoUserDetails.createBasicArgeoUser(authentication);
	}
}
