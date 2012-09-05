/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.server.dao;

import java.util.List;

/** Minimal generic DAO for easy to implements objects <-> storage mapping. */
public interface LightDaoSupport {
	/** Retrieve an object of a given type by its unique key. */
	public <T> T getByKey(Class<T> clss, Object key);

	/** Retrieve an object of a given type by the value of one of its fields. */
	public <T> T getByField(Class<T> clss, String field, Object value);

	/** List all objects, optionally filtering them (implementation dependent) */
	public <T> List<T> list(Class<T> clss, Object filter);

	/** Lis all supported object types. */
	public List<Class<?>> getSupportedClasses();

	/** Save or update an object */
	public void saveOrUpdate(Object key, Object value, Class<?> clss);
}
