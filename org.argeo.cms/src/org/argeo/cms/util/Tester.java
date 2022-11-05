package org.argeo.cms.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A generic tester based on Java assertions and functional programming. */
public class Tester {
	private Map<String, TesterStatus> results = Collections.synchronizedSortedMap(new TreeMap<>());

	private ClassLoader classLoader;

	/** Use {@link Thread#getContextClassLoader()} by default. */
	public Tester() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public Tester(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void execute(String className) {
		Class<?> clss;
		try {
			clss = classLoader.loadClass(className);
			boolean assertionsEnabled = clss.desiredAssertionStatus();
			if (!assertionsEnabled)
				throw new IllegalStateException("Test runner " + getClass().getName()
						+ " requires Java assertions to be enabled. Call the JVM with the -ea argument.");
		} catch (Exception e1) {
			throw new IllegalArgumentException("Cannot initalise test for " + className, e1);

		}
		List<Method> methods = findMethods(clss);
		if (methods.size() == 0)
			throw new IllegalArgumentException("No test method found in " + clss);
		// TODO make order more predictable?
		for (Method method : methods) {
			String uid = method.getDeclaringClass().getName() + "#" + method.getName();
			TesterStatus testStatus = new TesterStatus(uid);
			Object obj = null;
			try {
				beforeTest(uid, method);
				obj = clss.getDeclaredConstructor().newInstance();
				method.invoke(obj);
				testStatus.setPassed();
				afterTestPassed(uid, method, obj);
			} catch (Exception e) {
				testStatus.setFailed(e);
				afterTestFailed(uid, method, obj, e);
			} finally {
				results.put(uid, testStatus);
			}
		}
	}

	protected void beforeTest(String uid, Method method) {
		// System.out.println(uid + ": STARTING");
	}

	protected void afterTestPassed(String uid, Method method, Object obj) {
		System.out.println(uid + ": PASSED");
	}

	protected void afterTestFailed(String uid, Method method, Object obj, Throwable e) {
		System.out.println(uid + ": FAILED");
		e.printStackTrace();
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

		Tester test = new Tester();
		try {
			test.execute(className);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Map<String, TesterStatus> r = test.results;
		for (String uid : r.keySet()) {
			TesterStatus testStatus = r.get(uid);
			System.out.println(testStatus);
		}
	}

	public static String usage() {
		return "java " + Tester.class.getName() + " [test class name]";

	}
}
