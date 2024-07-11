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
	String OSGI_STORAGE_DIRNAME = "osgi";
	String DATA = "data";
	String SHARED = "shared";

	public void startRuntime(String relPath, Consumer<Map<String, String>> configCallback);

	public void closeRuntime(String relPath, boolean async);

	default void startRuntime(String relPath, String props) {
		startRuntime(relPath, (config) -> {
			loadProperties(config, props);
		});
	}

	static void loadProperties(Map<String, String> config, Properties properties) {
		for (Object key : properties.keySet()) {
			config.put(key.toString(), properties.getProperty(key.toString()));
		}
	}

	static void loadProperties(Map<String, String> config, String props) {
		Properties properties = new Properties();
		try (Reader reader = new StringReader(props)) {
			properties.load(reader);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load properties", e);
		}
		loadProperties(config, properties);
	}

	static void loadProperties(Map<String, String> config, InputStream in) throws IOException {
		Properties properties = new Properties();
		properties.load(in);
		loadProperties(config, properties);
	}

	static void loadDefaults(Map<String, String> config) {
		try (InputStream in = RuntimeManager.class.getResourceAsStream("defaults.ini")) {
			loadProperties(config, in);
		} catch (IOException e) {
			throw new IllegalStateException("Could not load runtime defaults", e);
		}
	}

	/**
	 * Load configs recursively starting with the parent directories, until a
	 * jvm.args file is found.
	 */
	@Deprecated
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
	 * starts with a '+' character, it is expected that the last character is a
	 * separator and it will be prepended to the existing value.
	 */
	@Deprecated
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
