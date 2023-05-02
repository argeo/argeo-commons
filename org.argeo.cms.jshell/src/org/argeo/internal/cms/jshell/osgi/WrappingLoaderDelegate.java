package org.argeo.internal.cms.jshell.osgi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;

/** A {@link LoaderDelegate} using a parent {@link ClassLoader}. */
class WrappingLoaderDelegate implements LoaderDelegate {
	private final WrappingClassloader loader;
	private final Map<String, Class<?>> klasses = new HashMap<>();

	private static class WrappingClassloader extends SecureClassLoader {

		private final Map<String, ClassFile> classFiles = new HashMap<>();

		public WrappingClassloader(ClassLoader parent) {
			super(parent);
		}

		private class ResourceURLStreamHandler extends URLStreamHandler {

			private final String name;

			ResourceURLStreamHandler(String name) {
				this.name = name;
			}

			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				return new URLConnection(u) {
					private InputStream in;
					private Map<String, List<String>> fields;
					private List<String> fieldNames;

					@Override
					public void connect() {
						if (connected) {
							return;
						}
						connected = true;
						ClassFile file = classFiles.get(name);
						in = new ByteArrayInputStream(file.data);
						fields = new LinkedHashMap<>();
						fields.put("content-length", List.of(Integer.toString(file.data.length)));
						Instant instant = new Date(file.timestamp).toInstant();
						ZonedDateTime time = ZonedDateTime.ofInstant(instant, ZoneId.of("GMT"));
						String timeStamp = DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
						fields.put("date", List.of(timeStamp));
						fields.put("last-modified", List.of(timeStamp));
						fieldNames = new ArrayList<>(fields.keySet());
					}

					@Override
					public InputStream getInputStream() throws IOException {
						connect();
						return in;
					}

					@Override
					public String getHeaderField(String name) {
						connect();
						return fields.getOrDefault(name, List.of()).stream().findFirst().orElse(null);
					}

					@Override
					public Map<String, List<String>> getHeaderFields() {
						connect();
						return fields;
					}

					@Override
					public String getHeaderFieldKey(int n) {
						return n < fieldNames.size() ? fieldNames.get(n) : null;
					}

					@Override
					public String getHeaderField(int n) {
						String name = getHeaderFieldKey(n);

						return name != null ? getHeaderField(name) : null;
					}

				};
			}
		}

		void declare(String name, byte[] bytes) {
			classFiles.put(toResourceString(name), new ClassFile(bytes, System.currentTimeMillis()));
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			ClassFile file = classFiles.get(toResourceString(name));
			if (file == null) {
				return super.findClass(name);
			}
			return super.defineClass(name, file.data, 0, file.data.length, (CodeSource) null);
		}

		@Override
		public URL findResource(String name) {
			URL u = doFindResource(name);
			return u != null ? u : super.findResource(name);
		}

		@Override
		public Enumeration<URL> findResources(String name) throws IOException {
			URL u = doFindResource(name);
			Enumeration<URL> sup = super.findResources(name);

			if (u == null) {
				return sup;
			}

			List<URL> result = new ArrayList<>();

			while (sup.hasMoreElements()) {
				result.add(sup.nextElement());
			}

			result.add(u);

			return Collections.enumeration(result);
		}

		private URL doFindResource(String name) {
			if (classFiles.containsKey(name)) {
				try {
					return new URL(null, new URI("jshell", null, "/" + name, null).toString(),
							new ResourceURLStreamHandler(name));
				} catch (MalformedURLException | URISyntaxException ex) {
					throw new InternalError(ex);
				}
			}

			return null;
		}

		private String toResourceString(String className) {
			return className.replace('.', '/') + ".class";
		}

		private static class ClassFile {
			public final byte[] data;
			public final long timestamp;

			ClassFile(byte[] data, long timestamp) {
				this.data = data;
				this.timestamp = timestamp;
			}

		}
	}

	public WrappingLoaderDelegate(ClassLoader parentClassLoader) {
		this.loader = new WrappingClassloader(parentClassLoader);

		Thread.currentThread().setContextClassLoader(loader);
	}

	@Override
	public void load(ClassBytecodes[] cbcs) throws ClassInstallException, EngineTerminationException {
		boolean[] loaded = new boolean[cbcs.length];
		try {
			for (ClassBytecodes cbc : cbcs) {
				loader.declare(cbc.name(), cbc.bytecodes());
			}
			for (int i = 0; i < cbcs.length; ++i) {
				ClassBytecodes cbc = cbcs[i];
				Class<?> klass = loader.loadClass(cbc.name());
				klasses.put(cbc.name(), klass);
				loaded[i] = true;
				// Get class loaded to the point of, at least, preparation
				klass.getDeclaredMethods();
			}
		} catch (Throwable ex) {
			throw new ClassInstallException("load: " + ex.getMessage(), loaded);
		}
	}

	@Override
	public void classesRedefined(ClassBytecodes[] cbcs) {
		for (ClassBytecodes cbc : cbcs) {
			loader.declare(cbc.name(), cbc.bytecodes());
		}
	}

	@Override
	public void addToClasspath(String cp) {
		// ignore
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> klass = klasses.get(name);
		if (klass == null) {
			throw new ClassNotFoundException(name + " not found");
		} else {
			return klass;
		}
	}

}
