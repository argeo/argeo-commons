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
package org.argeo.server.hibernate;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.server.dao.LightDaoSupport;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class LightDaoInterceptor extends EmptyInterceptor {
	private final static Log log = LogFactory.getLog(LightDaoInterceptor.class);

	private static final long serialVersionUID = 1L;

	public final static String DEFAULT_EXTERNAL_SUFFIX = "_external";

	private String externalSuffix = DEFAULT_EXTERNAL_SUFFIX;

	private LightDaoSupport lightDaoSupport;

	private List<Class<?>> classes = new ArrayList<Class<?>>();

	private Map<Class<?>, String> businessIdFields = new HashMap<Class<?>, String>();

	/** internal */
	private final Map<Class<?>, Map<Serializable, Object>> bidMappings = new HashMap<Class<?>, Map<Serializable, Object>>();

	@Override
	public Object getEntity(String entityName, Serializable id) {
		Class<?> clss = findSupportingClass(entityName);
		Object res = null;
		if (clss != null) {
			if (businessIdFields.containsKey(clss)) {
				String field = businessIdFields.get(clss);
				Object value = bidMappings.get(clss).get(id);
				res = lightDaoSupport.getByField(clss, field, value);
				if (log.isTraceEnabled())
					log.debug("Got entity " + clss + " (" + field + "=" + value
							+ ")");
			} else {
				res = lightDaoSupport.getByKey(clss, id);
				if (log.isTraceEnabled())
					log.debug("Got entity " + clss + " (id=" + id + ")");
			}
		} else {
			res = super.getEntity(entityName, id);
		}
		return res;
	}

	@Override
	public String getEntityName(Object object) {
		if (supports(object)) {
			return toExternalName(object.getClass());
		} else {
			return super.getEntityName(object);
		}
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (supports(entity)) {
			Class<?> clss = entity.getClass();
			if (businessIdFields.containsKey(clss)) {
				if (!bidMappings.containsKey(clss))
					bidMappings.put(clss, new HashMap<Serializable, Object>());
				BeanWrapper bw = new BeanWrapperImpl(entity);
				Object bid = bw.getPropertyValue(businessIdFields.get(clss));
				bidMappings.get(clss).put(id, bid);
				if (log.isTraceEnabled())
					log.debug("Mapped tid " + id + " with bid " + bid + " for "
							+ clss);
			}
		}
		return super.onSave(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		Class<?> clss = entity.getClass();
		Object source = null;
		if (lightDaoSupport.getSupportedClasses().contains(clss)) {
			if (businessIdFields.containsKey(clss)) {
				String field = businessIdFields.get(clss);
				Object value = bidMappings.get(clss).get(id);
				source = lightDaoSupport.getByField(clss, field, value);
				if (log.isTraceEnabled())
					log.debug("Loading entity " + clss + " (" + field + "="
							+ value + ")");
			} else {
				source = lightDaoSupport.getByKey(clss, id);
				if (log.isTraceEnabled())
					log.debug("Loading entity " + clss + " (id=" + id + ")");
			}
		}

		if (source != null) {
			BeanWrapper bwTarget = new BeanWrapperImpl(entity);
			BeanWrapper bwSource = new BeanWrapperImpl(source);
			for (PropertyDescriptor pd : bwTarget.getPropertyDescriptors()) {
				String propName = pd.getName();
				if (bwSource.isReadableProperty(propName)
						&& bwTarget.isWritableProperty(propName)) {
					bwTarget.setPropertyValue(propName, bwSource
							.getPropertyValue(propName));
					if (log.isTraceEnabled())
						log.debug("Loaded property " + propName + " for class "
								+ clss + " (id=" + id + ")");
				}
			}

			return true;
		} else {
			// res = super.getEntity(entityName, id);
			return super.onLoad(entity, id, state, propertyNames, types);
		}
	}

	protected Boolean supports(Object object) {
		if (classes.contains(object.getClass()))
			return lightDaoSupport.getSupportedClasses().contains(
					object.getClass());
		else
			return false;
	}

	/** @return null if not found */
	protected Class<?> findSupportingClass(String entityName) {
		for (Class<?> clss : lightDaoSupport.getSupportedClasses()) {
			if (toExternalName(clss).equals(entityName)) {
				if (classes.contains(clss))
					return clss;
			}
		}
		return null;
	}

	protected final String toExternalName(Class<?> clss) {
		return clss.getSimpleName() + externalSuffix;
	}

	public void setExternalSuffix(String externalSuffix) {
		this.externalSuffix = externalSuffix;
	}

	public void setLightDaoSupport(LightDaoSupport lightDaoSupport) {
		this.lightDaoSupport = lightDaoSupport;
	}

	public void setClasses(List<Class<?>> classes) {
		this.classes = classes;
	}

	public void setBusinessIdFields(Map<Class<?>, String> businessIdFields) {
		this.businessIdFields = businessIdFields;
	}

}
