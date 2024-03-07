package org.argeo.api.init;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/** Dynamically manages multiple runtimes within a single JVM. */
public interface RuntimeManager {
	String JVM_ARGS = "jvm.args";
	String STATE = "state";
	String DATA = "data";

	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback);

	public void closeRuntime(String relPath, boolean async);

	static void loadConfig(Path dir, Map<String, String> config) {
			try {
	//			System.out.println("Load from " + dir);
				Path jvmArgsPath = dir.resolve(RuntimeManager.JVM_ARGS);
				if (!Files.exists(jvmArgsPath)) {
					// load from parent directory
					loadConfig(dir.getParent(), config);
				}
	
				if (Files.exists(dir))
					for (Path p : Files.newDirectoryStream(dir, "*.ini")) {
						Properties props = new Properties();
						try (InputStream in = Files.newInputStream(p)) {
							props.load(in);
						}
						for (Object key : props.keySet()) {
							config.put(key.toString(), props.getProperty(key.toString()));
						}
					}
			} catch (IOException e) {
				throw new UncheckedIOException("Cannot load configuration from " + dir, e);
			}
		}
}
