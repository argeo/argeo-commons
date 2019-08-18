package org.argeo.util.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A generic tester based on Java assertions and functional programming. */
public class Test {
	private Map<String, TestStatus> results = Collections.synchronizedSortedMap(new TreeMap<>());

	protected void execute(String className) throws Throwable {
		ClassLoader classLoader = Test.class.getClassLoader();
		Class<?> clss = classLoader.loadClass(className);
		boolean assertionsEnabled = clss.desiredAssertionStatus();
		if (!assertionsEnabled)
			throw new IllegalStateException("Test runner " + getClass().getName()
					+ " requires Java assertions to be enabled. Call the JVM with the -ea argument.");
		Object obj = clss.getDeclaredConstructor().newInstance();
		List<Method> methods = findMethods(clss);
		if (methods.size() == 0)
			throw new IllegalArgumentException("No test method found in " + clss);
		// TODO make order more predictable?
		for (Method method : methods) {
			String uid = method.getDeclaringClass().getName() + "#" + method.getName();
			TestStatus testStatus = new TestStatus(uid);
			try {
				method.invoke(obj);
				testStatus.setPassed();
			} catch (Exception e) {
				testStatus.setFailed(e);
			} finally {
				results.put(uid, testStatus);
			}
		}
	}

	protected List<Method> findMethods(Class<?> clss) {
		List<Method> methods = new ArrayList<Method>();
//		Method call = getMethod(clss, "call");
//		if (call != null)
//			methods.add(call);
//
		for (Method method : clss.getMethods()) {
			if (method.getName().startsWith("test")) {
				methods.add(method);
			}
		}
		return methods;
	}

	protected Method getMethod(Class<?> clss, String name, Class<?>... parameterTypes) {
		try {
			return clss.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) {
		// deal with arguments
		String className;
		if (args.length < 1) {
			System.err.println(usage());
			System.exit(1);
			throw new IllegalArgumentException();
		} else {
			className = args[0];
		}

		Test test = new Test();
		try {
			test.execute(className);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Map<String, TestStatus> r = test.results;
		for (String uid : r.keySet()) {
			TestStatus testStatus = r.get(uid);
			System.out.println(testStatus);
		}
	}

	public static String usage() {
		return "java " + Test.class.getName() + " [test class name]";

	}
}
