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
package org.argeo.node;

/** JCR types in the http://www.argeo.org/argeo namespace */
public interface ArgeoTypes {
	public final static String ARGEO_LINK = "argeo:link";
	public final static String ARGEO_USER_HOME = "argeo:userHome";
	public final static String ARGEO_USER_PROFILE = "argeo:userProfile";
	public final static String ARGEO_REMOTE_REPOSITORY = "argeo:remoteRepository";
	public final static String ARGEO_PREFERENCE_NODE = "argeo:preferenceNode";

	// data model
	public final static String ARGEO_DATA_MODEL = "argeo:dataModel";
	
	// tabular
	public final static String ARGEO_TABLE = "argeo:table";
	public final static String ARGEO_COLUMN = "argeo:column";
	public final static String ARGEO_CSV = "argeo:csv";

	// crypto
	public final static String ARGEO_ENCRYPTED = "argeo:encrypted";
	public final static String ARGEO_PBE_SPEC = "argeo:pbeSpec";

}
