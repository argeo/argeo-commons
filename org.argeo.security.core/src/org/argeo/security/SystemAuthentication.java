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

/**
 * Marks a system authentication, that is which did not require a login process.
 */
public interface SystemAuthentication {
	/** 'admin' for consistency with JCR */
	public final static String USERNAME_SYSTEM = "admin";
	public final static String ROLE_SYSTEM = "ROLE_SYSTEM";
	public final static String SYSTEM_KEY_PROPERTY = "argeo.security.systemKey";
}
