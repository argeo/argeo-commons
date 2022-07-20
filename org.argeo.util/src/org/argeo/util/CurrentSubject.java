package org.argeo.util;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import javax.security.auth.Subject;

/**
 * Prepare evolution of Java APIs introduced in JDK 18, as these static methods
 * will be added to {@link Subject}.
 */
@SuppressWarnings("removal")
public class CurrentSubject {

	private final static boolean useThreadLocal = Boolean
			.parseBoolean(System.getProperty("jdk.security.auth.subject.useTL"));

	private final static InheritableThreadLocal<Subject> current = new InheritableThreadLocal<>();

	public static Subject current() {
		if (useThreadLocal) {
			return current.get();
		} else {// legacy
			Subject subject = Subject.getSubject(AccessController.getContext());
			return subject;
		}
	}

	public static <T> T callAs(Subject subject, Callable<T> action) {
		if (useThreadLocal) {
			Subject previous = current();
			current.set(subject);
			try {
				return action.call();
			} catch (Exception e) {
				throw new CompletionException("Failed to execute action for " + subject, e);
			} finally {
				current.set(previous);
			}
		} else {// legacy
			try {
				return Subject.doAs(subject, new PrivilegedExceptionAction<T>() {

					@Override
					public T run() throws Exception {
						return action.call();
					}

				});
			} catch (PrivilegedActionException e) {
				throw new CompletionException("Failed to execute action for " + subject, e.getCause());
			} catch (Exception e) {
				throw new CompletionException("Failed to execute action for " + subject, e);
			}
		}
	}

	/** Singleton. */
	private CurrentSubject() {
	}

}
