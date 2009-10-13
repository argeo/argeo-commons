package org.argeo.server.hibernate;

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
				if (log.isDebugEnabled())
					log.debug("Got entity " + clss + " (" + field + "=" + value
							+ ")");
			} else {
				res = lightDaoSupport.getByKey(clss, id);
				if (log.isDebugEnabled())
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
				if (log.isDebugEnabled())
					log.debug("Mapped tid " + id + " with bid " + bid + " for "
							+ clss);
			}
		}
		return super.onSave(entity, id, state, propertyNames, types);
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
