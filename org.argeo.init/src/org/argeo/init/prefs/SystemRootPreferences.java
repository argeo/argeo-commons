package org.argeo.init.prefs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public class SystemRootPreferences extends AbstractPreferences implements Consumer<AbstractPreferences> {
	private CompletableFuture<AbstractPreferences> singleChild;

	protected SystemRootPreferences() {
		super(null, "");
	}

	@Override
	public void accept(AbstractPreferences t) {
		this.singleChild.complete(t);
	}

	/*
	 * ABSTRACT PREFERENCES
	 */

	@Override
	protected void putSpi(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getSpi(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeSpi(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return new String[0];
	}

	/** Will block. */
	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		String childName;
		try {
			childName = singleChild.get().name();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot get child preferences name", e);
		}
		return new String[] { childName };
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		String childName;
		try {
			childName = singleChild.get().name();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("Cannot get child preferences name", e);
		}
		if (!childName.equals(name))
			throw new IllegalArgumentException("Child name is " + childName + ", not " + name);
		return null;
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
	}

}
