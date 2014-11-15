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
package org.argeo.security.ldap;

import org.springframework.security.providers.ldap.authenticator.LdapShaPasswordEncoder;

/**
 * {@link LdapShaPasswordEncoder} allowing to configure the usage of salt (APache
 * Directory Server 1.0 does not support bind with SSHA)
 */
public class ArgeoLdapShaPasswordEncoder extends LdapShaPasswordEncoder {
	private Boolean useSalt = true;

	@Override
	public String encodePassword(String rawPass, Object salt) {
		return super.encodePassword(rawPass, useSalt ? salt : null);
	}

	public void setUseSalt(Boolean useSalt) {
		this.useSalt = useSalt;
	}

}
