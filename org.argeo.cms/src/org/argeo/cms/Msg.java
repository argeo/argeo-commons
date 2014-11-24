package org.argeo.cms;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.rap.rwt.RWT;

/** A single message to be internationalised. */
public class Msg {
	private String id;
	private ClassLoader classLoader;
	private final Object defaultLocal;

	public Msg() {
		defaultLocal = null;
	}

	public Msg(Object defaultMessage) {
		this.defaultLocal = defaultMessage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Object getDefault() {
		return defaultLocal;
	}

	public String toString() {
		return local().toString();
	}

	/** When used as the first word of a sentence. */
	public String lead() {
		String raw = toString();
		return raw.substring(0, 1).toUpperCase(RWT.getLocale())
				+ raw.substring(1);
	}

	public Object local() {
		CmsSession cmSession = CmsSession.current.get();
		Object local = cmSession.local(this);
		if (local == null)
			local = getDefault();
		if (local == null)
			throw new CmsException("No translation found for " + id);
		return local;
	}

	public static void init(Class<?> clss) {
		final Field[] fieldArray = clss.getDeclaredFields();
		ClassLoader loader = clss.getClassLoader();

		for (Field field : fieldArray) {
			if (Modifier.isStatic(field.getModifiers())
					&& field.getType().isAssignableFrom(Msg.class)) {
				try {
					Object obj = field.get(null);
					String id = clss.getCanonicalName() + "." + field.getName();
					obj.getClass().getMethod("setId", String.class)
							.invoke(obj, id);
					obj.getClass()
							.getMethod("setClassLoader", ClassLoader.class)
							.invoke(obj, loader);
				} catch (Exception e) {
					throw new CmsException("Cannot prepare field " + field);
				}
			}
		}
	}
}
