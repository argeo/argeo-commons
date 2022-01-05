package org.argeo.util.register;

import java.util.Map;

/** A dynamic register of objects. */
public interface Register {
	<T> Singleton<T> set(T obj, Class<T> clss, Map<String, Object> attributes, Class<?>... classes);

	void shutdown();
}
