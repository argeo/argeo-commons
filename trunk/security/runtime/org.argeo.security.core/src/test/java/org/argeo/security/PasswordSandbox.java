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
package org.argeo.security;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.springframework.security.providers.ldap.authenticator.LdapShaPasswordEncoder;

public class PasswordSandbox {
	public static void main(String[] args) {
		try {
			// Tested password
			String pwdPlain = "demo";

			// Check Java generated values
			LdapShaPasswordEncoder lspe = new LdapShaPasswordEncoder();
			String pwdLdapShaBase64 = lspe.encodePassword(pwdPlain, null);
			System.out.println("pwdLdapShaBase64:\t\t" + pwdLdapShaBase64);

			String pwdShaBase64 = pwdLdapShaBase64.substring("{SHA}".length());
			System.out.println("pwdShaBase64:\t\t\t" + pwdShaBase64);

			byte[] pwdShaArray = Base64.decodeBase64(pwdShaBase64.getBytes());
			String pwdShaHex = new String(Hex.encodeHex(pwdShaArray));
			System.out.println("pwdShaHex:\t\t\t" + pwdShaHex);

			// Check that we can use JavaScript generated values in Hex
			String jsShaHex = "89e495e7941cf9e40e6980d14a16bf023ccd4c91";
			System.out.println("jsShaHex:\t\t\t" + pwdShaHex);
			System.out.println("pwdShaHex==jsShaHex:\t\t"
					+ (pwdShaHex.equals(jsShaHex)));

			byte[] jsShaArray = Hex.decodeHex(jsShaHex.toCharArray());
			String jsShaBase64 = new String(Base64.encodeBase64(jsShaArray));
			System.out.println("jsShaBase64:\t\t\t" + jsShaBase64);
			System.out.println("pwdShaBase64==jsShaBase64:\t"
					+ (pwdShaBase64.equals(jsShaBase64)));
		} catch (DecoderException e) {
			e.printStackTrace();
		}

	}

}