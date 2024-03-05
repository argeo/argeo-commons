package org.argeo.api.init;

import java.util.Map;
import java.util.function.Consumer;

/** Dynamically manages multiple runtimes within a single JVM. */
public interface RuntimeManager {
	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback);

	public void closeRuntime(String relPath, boolean async);
}
