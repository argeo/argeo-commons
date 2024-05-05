package org.argeo.api.init;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
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

	default void startRuntime(String relPath, String props) {
		Properties properties = new Properties();
		try (Reader reader = new StringReader(props)) {
			properties.load(reader);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load properties", e);
		}
		startRuntime(relPath, (config) -> {
			for (Object key : properties.keySet()) {
				config.put(key.toString(), properties.getProperty(key.toString()));
			}
		});
	}

	/**
	 * Load configs recursively starting with the parent directories, until a
	 * jvm.args file is found.
	 */
	static void loadConfig(Path dir, Map<String, String> config) {
		try {
			Path jvmArgsPath = dir.resolve(RuntimeManager.JVM_ARGS);
			if (!Files.exists(jvmArgsPath)) {
				// load from parent directory
				loadConfig(dir.getParent(), config);
			}

			if (Files.exists(dir))
				for (Path p : Files.newDirectoryStream(dir, "*.ini")) {
					try (InputStream in = Files.newInputStream(p)) {
						loadConfig(in, config);
					}
				}
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot load configuration from " + dir, e);
		}
	}

	/**
	 * Load config from a {@link Properties} formatted stream. If a property value
	 * starts with a '+' character, itis expected that the last character is a
	 * separator and it will be prepended to the existing value.
	 */
	static void loadConfig(InputStream in, Map<String, String> config) throws IOException {
		Properties props = new Properties();
		props.load(in);
		for (Object k : props.keySet()) {
			String key = k.toString();
			String value = props.getProperty(key);
			if (value.length() > 1 && '+' == value.charAt(0)) {
				String currentValue = config.get(key);
				if (currentValue == null || "".equals(currentValue)) {
					// remove the + and the trailing separator
					value = value.substring(1, value.length() - 1);
					config.put(key, value);
				} else {
					// remove the + but keep the trailing separator
					value = value.substring(1);
					config.put(key, value + currentValue);
				}
			} else {
				config.put(key, value);
			}
		}
	}
}
