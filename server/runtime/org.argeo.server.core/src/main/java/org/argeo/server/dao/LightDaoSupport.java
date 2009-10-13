package org.argeo.server.dao;

import java.util.List;

public interface LightDaoSupport {
	public <T> T getByKey(Class<T> clss, Object key);

	public <T> T getByField(Class<T> clss, String field, Object value);

	public <T> List<T> list(Class<T> clss, Object filter);

	public List<Class<?>> getSupportedClasses();
}
