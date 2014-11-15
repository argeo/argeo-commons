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
package org.argeo.security.jackrabbit;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.spi.Path;

/**
 * Intermediary class in order to have a consistent naming in config files. Does
 * nothing for the time being, but may in the future.
 */
public class ArgeoAccessManager extends DefaultAccessManager {

	@Override
	public boolean canRead(Path itemPath, ItemId itemId)
			throws RepositoryException {
		return super.canRead(itemPath, itemId);
	}

	@Override
	public Privilege[] getPrivileges(String absPath)
			throws PathNotFoundException, RepositoryException {
		return super.getPrivileges(absPath);
	}

	@Override
	public boolean hasPrivileges(String absPath, Privilege[] privileges)
			throws PathNotFoundException, RepositoryException {
		return super.hasPrivileges(absPath, privileges);
	}

}
