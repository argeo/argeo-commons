package org.argeo.init.prefs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class ThinPreferencesFactory implements PreferencesFactory {
	private static CompletableFuture<ThinPreferencesFactory> INSTANCE = new CompletableFuture<>();

	private SystemRootPreferences systemRootPreferences;

	public ThinPreferencesFactory() {
		systemRootPreferences = new SystemRootPreferences();
		if (INSTANCE.isDone())
			throw new IllegalStateException(
					"There is already a " + ThinPreferencesFactory.class.getName() + " instance.");
		INSTANCE.complete(this);
	}

	@Override
	public Preferences systemRoot() {
		return systemRootPreferences;
	}

	@Override
	public Preferences userRoot() {
		throw new UnsupportedOperationException();
	}

	public SystemRootPreferences getSystemRootPreferences() {
		return systemRootPreferences;
	}

	public static ThinPreferencesFactory getInstance() {
		try {
			return INSTANCE.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot get " + ThinPreferencesFactory.class.getName() + " instance.", e);
		}
	}
}
