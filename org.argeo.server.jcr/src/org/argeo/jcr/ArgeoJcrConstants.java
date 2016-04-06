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
package org.argeo.jcr;

import javax.jcr.Repository;

/** Argeo model specific constants */
public interface ArgeoJcrConstants {
	public final static String ARGEO_BASE_PATH = "/argeo:system";
	public final static String DATA_MODELS_BASE_PATH = ARGEO_BASE_PATH + "/argeo:dataModels";
	public final static String PEOPLE_BASE_PATH = ARGEO_BASE_PATH + "/argeo:people";

	// parameters (typically for call to a RepositoryFactory)
	/** Key for a JCR repository alias */
	public final static String JCR_REPOSITORY_ALIAS = "argeo.jcr.repository.alias";
	/** Key for a JCR repository URI */
	public final static String JCR_REPOSITORY_URI = "argeo.jcr.repository.uri";

	// standard aliases
	/**
	 * Reserved alias for the "node" {@link Repository}, that is, the default
	 * JCR repository.
	 */
	public final static String ALIAS_NODE = "node";
	public final static String BASE_REPO_PID = "argeo.repo.";
	public final static String REPO_PID_NODE = BASE_REPO_PID + ALIAS_NODE;

}
