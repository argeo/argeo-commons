package org.argeo.cms.i18n;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.ResourceBundle;

import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.specific.UiContext;

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
		return lead(UiContext.getLocale());
	}

	public String lead(Locale locale) {
		return lead(this, locale);
	}

	private static String lead(Msg msg, Locale locale) {
		String raw = msg.local(locale).toString();
		return lead(raw, locale);
	}

	public static String lead(String raw, Locale locale) {
		return raw.substring(0, 1).toUpperCase(locale) + raw.substring(1);
	}

	public Object local() {
		Object local = local(this);
		if (local == null)
			local = getDefault();
		if (local == null)
			throw new CmsException("No translation found for " + id);
		return local;
	}

	public Object local(Locale locale) {
		Object local = local(this, locale);
		if (local == null)
			local = getDefault();
		if (local == null)
			throw new CmsException("No translation found for " + id);
		return local;
	}

	private static Object local(Msg msg) {
		Locale locale = UiContext.getLocale();
		return local(msg, locale);
	}

	private static Object local(Msg msg, Locale locale) {
		String key = msg.getId();
		int lastDot = key.lastIndexOf('.');
		String className = key.substring(0, lastDot);
		String fieldName = key.substring(lastDot + 1);
		ResourceBundle rb = ResourceBundle.getBundle(className, locale,
				msg.getClassLoader());
		return rb.getString(fieldName);
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
