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
package org.argeo.cms.ui.workbench.internal.jcr;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

/** Allows to register repositories by name. */
public interface RepositoryRegister extends RepositoryFactory {
	/**
	 * The registered {@link Repository} as a read-only map. Note that this
	 * method should be called for each access in order to be sure to be up to
	 * date in case repositories have registered/unregistered
	 */
	public Map<String, Repository> getRepositories();
}
