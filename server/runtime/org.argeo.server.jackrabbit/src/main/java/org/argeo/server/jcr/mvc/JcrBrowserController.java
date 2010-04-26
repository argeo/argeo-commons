package org.argeo.server.jcr.mvc;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

@Controller
public class JcrBrowserController implements JcrMvcConstants {

	@RequestMapping("/getJcrItem.*")
	public Item getJcrItem(WebRequest webRequest,
			@RequestParam("path") String path) throws RepositoryException {
		return ((Session) webRequest.getAttribute(REQUEST_ATTR_SESSION,
				RequestAttributes.SCOPE_REQUEST)).getItem(path);
	}

}
