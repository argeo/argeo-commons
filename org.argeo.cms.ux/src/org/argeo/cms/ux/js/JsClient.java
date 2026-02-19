package org.argeo.cms.ux.js;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A remote JavaScript view (typically in a web browser) which is tightly
 * integrated with a local UX part.
 */
public interface JsClient {

	/*
	 * TO IMPLEMENT
	 */

	/**
	 * Execute this JavaScript on the client side after making sure that the page
	 * has been loaded and the map object has been created.
	 * 
	 * @param js   the JavaScript code, possibly formatted according to
	 *             {@link String#format}, with {@link Locale#ROOT} as locale (for
	 *             stability of decimal separator, as expected by JavaScript.
	 * @param args the optional arguments of
	 *             {@link String#format(String, Object...)}
	 */
	Object evaluate(String js, Object... args);

	/**
	 * Executes this JavaScript without expecting a return value.
	 * 
	 * @param js   the JavaScript code, possibly formatted according to
	 *             {@link String#format}, with {@link Locale#ROOT} as locale (for
	 *             stability of decimal separator, as expected by JavaScript.
	 * @param args the optional arguments of
	 *             {@link String#format(String, Object...)}
	 */
	void execute(String js, Object... args);

	/** @return the globally usable function name. */
	String createJsFunction(String name, Function<Object[], Object> toDo);

	/** Get a global variable name. */
	String getJsVarName(String name);

	/**
	 * Completion stage when the client is ready (typically the page has loaded in
	 * the browser).
	 */
	CompletionStage<Boolean> getReadyStage();

	/*
	 * DEFAULTS
	 */

	default Object callMethod(String jsObject, String methodCall, Object... args) {
		return evaluate(jsObject + '.' + methodCall, args);
	}

	default void executeMethod(String jsObject, String methodCall, Object... args) {
		execute(jsObject + '.' + methodCall, args);
	}

	default boolean isInstanceOf(String reference, String jsClass) {
		try {
			return (Boolean) evaluate("return " + getJsVarName(reference) + " instanceof " + jsClass);
		} catch (Exception e) {
			// TODO better understand why instanceof is often failing with SWT Browser
			return false;
		}
	}

	/*
	 * UTILITIES
	 */

	static String toJsValue(Object o) {
		if (o instanceof CharSequence)
			return '\'' + o.toString() + '\'';
		else if (o instanceof Number)
			return o.toString();
		else if (o instanceof Boolean)
			return o.toString();
		else if (o instanceof Map map)
			return toJsMap(map);
		else if (o instanceof Object[] arr)
			return toJsArray(arr);
		else if (o instanceof int[] arr)
			return toJsArray(arr);
		else if (o instanceof long[] arr)
			return toJsArray(arr);
		else if (o instanceof double[] arr)
			return toJsArray(arr);
		else if (o instanceof AbstractJsObject jsObject) {
			if (jsObject.isNew())
				return jsObject.newJs();
			else
				return jsObject.getJsReference();
		} else if (o instanceof JsReference jsReference) {
			return jsReference.get();
		} else
			return '\'' + o.toString() + '\'';
	}

	static String toJsArgs(Object... arr) {
		StringJoiner sj = new StringJoiner(",");
		for (Object o : arr) {
			sj.add(toJsValue(o));
		}
		return sj.toString();
	}

	static String toJsArray(Object... arr) {
		StringJoiner sj = new StringJoiner(",", "[", "]");
		for (Object o : arr) {
			sj.add(toJsValue(o));
		}
		return sj.toString();
	}

	static String toJsArray(String... arr) {
		return toJsArray((Object[]) arr);
	}

	static String toJsArray(double... arr) {
		return Arrays.toString(arr);
	}

	static String toJsArray(long... arr) {
		return Arrays.toString(arr);
	}

	static String toJsArray(int... arr) {
		return Arrays.toString(arr);
	}

	static String toJsMap(Map<?, ?> map) {
		StringJoiner sj = new StringJoiner(",", "{", "}");
		// TODO escape forbidden characters
		for (Object key : map.keySet()) {
			sj.add("'" + key + "':" + toJsValue(map.get(key)));
		}
		return sj.toString();
	}

	static String escapeQuotes(String str) {
		return str.replace("'", "\\'").replace("\"", "\\\"");
	}

}
