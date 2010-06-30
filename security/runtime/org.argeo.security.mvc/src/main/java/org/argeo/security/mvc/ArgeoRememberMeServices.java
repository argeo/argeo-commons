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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.ui.rememberme.TokenBasedRememberMeServices;

public class ArgeoRememberMeServices extends TokenBasedRememberMeServices {
	public final static String DEFAULT_COOKIE_NAME = "ARGEO_SECURITY";

	public ArgeoRememberMeServices() {
		setCookieName(DEFAULT_COOKIE_NAME);
	}

	/**
	 * Sets a "cancel cookie" (with maxAge = 0) on the response to disable
	 * persistent logins.
	 * 
	 * @param request
	 * @param response
	 */
	protected void cancelCookie(HttpServletRequest request,
			HttpServletResponse response) {
		Cookie cookie = new Cookie(getCookieName(), null);
		cookie.setMaxAge(0);
		cookie.setPath("/");

		response.addCookie(cookie);
	}

	/**
	 * Sets the cookie on the response
	 * 
	 * @param tokens
	 *            the tokens which will be encoded to make the cookie value.
	 * @param maxAge
	 *            the value passed to {@link Cookie#setMaxAge(int)}
	 * @param request
	 *            the request
	 * @param response
	 *            the response to add the cookie to.
	 */
	protected void setCookie(String[] tokens, int maxAge,
			HttpServletRequest request, HttpServletResponse response) {
		String cookieValue = encodeCookie(tokens);
		Cookie cookie = new Cookie(getCookieName(), cookieValue);
		cookie.setMaxAge(maxAge);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

}
