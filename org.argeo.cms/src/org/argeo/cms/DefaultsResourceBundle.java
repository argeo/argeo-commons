package org.argeo.cms;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Vector;

/** Expose the default values as a {@link ResourceBundle} */
public class DefaultsResourceBundle extends ResourceBundle {

	@Override
	protected Object handleGetObject(String key) {
		Object obj;
		try {
			Field field = getClass().getField(key);
			obj = field.getType().getMethod("getDefault")
					.invoke(field.get(null));
		} catch (Exception e) {
			throw new CmsException("Cannot get default for " + key, e);
		}
		return obj;
	}

	@Override
	public Enumeration<String> getKeys() {
		Vector<String> res = new Vector<String>();
		final Field[] fieldArray = getClass().getDeclaredFields();

		for (Field field : fieldArray) {
			if (Modifier.isStatic(field.getModifiers())
					&& field.getType().isAssignableFrom(Msg.class)) {
				res.add(field.getName());
			}
		}
		return res.elements();
	}

}
