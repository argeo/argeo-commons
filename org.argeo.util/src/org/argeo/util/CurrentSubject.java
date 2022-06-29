package org.argeo.util;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import javax.security.auth.Subject;

/**
 * Prepare evotion of Java APIs introduced in JDK 18, as these static methods
 * will be added to {@link Subject}.
 */
@SuppressWarnings("removal")
public class CurrentSubject {

	/** Singleton. */
	private CurrentSubject() {

	}

	public static Subject current() {
		Subject subject = Subject.getSubject(AccessController.getContext());
		if (subject == null)
			throw new IllegalStateException("Cannot find related subject");
		return subject;
	}

	public static <T> T callAs(Subject subject, Callable<T> action) {
		try {
			return Subject.doAs(subject, new PrivilegedExceptionAction<T>() {
	
				@Override
				public T run() throws Exception {
					return action.call();
				}
	
			});
		} catch (PrivilegedActionException e) {
			throw new CompletionException("Failed to execute action for " + subject, e.getCause());
		}
	}
}
