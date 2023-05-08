package org.argeo.cms.jshell;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.CmsState;
import org.argeo.api.uuid.UuidFactory;
import org.argeo.cms.util.OS;
import org.argeo.internal.cms.jshell.osgi.OsgiExecutionControlProvider;
import org.osgi.framework.Bundle;

public class CmsJShell {
	private final static CmsLog log = CmsLog.getLog(CmsJShell.class);
	static ClassLoader loader = CmsJShell.class.getClassLoader();

	public static UuidFactory uuidFactory = null;

	private CmsState cmsState;

	private Map<Path, LocalJShellSession> localSessions = new HashMap<>();
	private Map<Path, Path> bundleDirs = new HashMap<>();

	private Path stateRunDir;
	private Path localBase;
	private Path linkedDir;

//	private String defaultBundle = "org.argeo.cms.cli";

	public void start() throws Exception {

		// Path localBase = cmsState.getStatePath("org.argeo.cms.jshell/local");
//		UUID stateUuid = cmsState.getUuid();

		// TODO better define application id, make it configurable
		String applicationID = cmsState.getStatePath("").getFileName().toString();

		// TODO centralise state run dir
		stateRunDir = OS.getRunDir().resolve(applicationID);
		localBase = stateRunDir.resolve("jsh");
		Files.createDirectories(localBase);

		linkedDir = Files.createSymbolicLink(cmsState.getStatePath("jsh"), localBase);

		log.info("Local JShell on " + localBase + ", linked to " + linkedDir);

		new Thread(() -> {
			try {
				WatchService watchService = FileSystems.getDefault().newWatchService();

				localBase.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_DELETE);
				try (DirectoryStream<Path> bundleSns = Files.newDirectoryStream(localBase)) {
					for (Path bundleSnDir : bundleSns) {
						addBundleSnDir(bundleSnDir);
						if (bundleDirs.containsKey(bundleSnDir)) {
							bundleSnDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
									StandardWatchEventKinds.ENTRY_DELETE);
						}
					}
				}

				WatchKey key;
				while ((key = watchService.take()) != null) {
					events: for (WatchEvent<?> event : key.pollEvents()) {
//						System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
						Path parent = (Path) key.watchable();
						// sessions
						if (Files.isSameFile(localBase, parent)) {
							Path bundleSnDir = localBase.resolve((Path) event.context());
							if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
								addBundleSnDir(bundleSnDir);
								if (bundleDirs.containsKey(bundleSnDir)) {
									bundleSnDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
											StandardWatchEventKinds.ENTRY_DELETE);
								}
							} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
							}
						} else {
							Path path = parent.resolve((Path) event.context());
							if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
								if (!Files.isDirectory(path)) {
									log.warn("Ignoring " + path + " as it is not a directory");
									continue events;
								}
								try {
									UUID.fromString(path.getFileName().toString());
								} catch (IllegalArgumentException e) {
									log.warn("Ignoring " + path + " as it is not named as UUID");
									continue events;
								}

								Path bundleIdDir = bundleDirs.get(parent);
								LocalJShellSession localSession = new LocalJShellSession(path, bundleIdDir);
								localSessions.put(path, localSession);
							} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
								// TODO clean up session
								LocalJShellSession localSession = localSessions.remove(path);
								localSession.cleanUp();
							}
						}
					}
					key.reset();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}, "JShell local sessions watcher").start();

		// thread context class loader should be where the service is defined
//		Thread.currentThread().setContextClassLoader(loader);
//		JavaShellToolBuilder builder = JavaShellToolBuilder.builder();
//
//		builder.start("--execution", "osgi:bundle(org.argeo.cms.jshell)");

	}

	private void addBundleSnDir(Path bundleSnDir) throws IOException {
		String symbolicName = bundleSnDir.getFileName().toString();
		Bundle fromBundle = OsgiExecutionControlProvider.getBundleFromSn(symbolicName);
		if (fromBundle == null) {
			log.error("Ignoring bundle " + symbolicName + " because it was not found");
			return;
		}
		Long bundleId = fromBundle.getBundleId();
		Path bundleIdDir = stateRunDir.resolve(bundleId.toString());
		Files.createDirectories(bundleIdDir);
		bundleDirs.put(bundleSnDir, bundleIdDir);
	}

//	public void startX(BundleContext bc) {
//		uuidFactory = new NoOpUuidFactory();
//
//		List<String> locations = new ArrayList<>();
//		for (Bundle bundle : bc.getBundles()) {
//			locations.add(bundle.getLocation());
////			System.out.println(bundle.getLocation());
//		}
//
//		CmsState cmsState = (CmsState) bc.getService(bc.getServiceReference("org.argeo.api.cms.CmsState"));
//		System.out.println(cmsState.getDeployProperties(CmsDeployProperty.HTTP_PORT.getProperty()));
//		System.out.println(cmsState.getUuid());
//
//		ExecutionControlProvider executionControlProvider = new ExecutionControlProvider() {
//			@Override
//			public String name() {
//				return "name";
//			}
//
//			@Override
//			public ExecutionControl generate(ExecutionEnv ee, Map<String, String> map) throws Throwable {
//				return new LocalExecutionControl(new WrappingLoaderDelegate(loader));
////				Thread.currentThread().setContextClassLoader(loader);
////				return new DirectExecutionControl();
//			}
//		};
//
////		Thread.currentThread().setContextClassLoader(loader);
//
//		try (JShell js = JShell.builder().executionEngine(executionControlProvider, null).build()) {
//			js.addToClasspath("/home/mbaudier/dev/git/unstable/output/a2/org.argeo.cms/org.argeo.api.cms.2.3.jar");
//			js.addToClasspath("/home/mbaudier/dev/git/unstable/output/a2/org.argeo.cms/org.argeo.cms.2.3.jar");
//			js.addToClasspath(
//					"/home/mbaudier/dev/git/unstable/output/a2/osgi/equinox/org.argeo.tp.osgi/org.eclipse.osgi.3.18.jar");
////			do {
//			System.out.print("Enter some Java code: ");
//			// String input = console.readLine();
//			String imports = """
//					import org.argeo.api.cms.*;
//					import org.argeo.cms.*;
//					import org.argeo.slc.jshell.*;
//					""";
//			js.eval(imports);
//			String input = """
//					var bc = org.osgi.framework.FrameworkUtil.getBundle(org.argeo.cms.CmsDeployProperty.class).getBundleContext();
//					var cmsState =(org.argeo.api.cms.CmsState) bc.getService(bc.getServiceReference("org.argeo.api.cms.CmsState"));
//					System.out.println(cmsState.getDeployProperties(org.argeo.cms.CmsDeployProperty.HTTP_PORT.getProperty()));
//					cmsState.getUuid();
//						""";
////				if (input == null) {
////					break;
////				}
//
//			input.lines().forEach((l) -> {
//
//				List<SnippetEvent> events = js.eval(l);
//				for (SnippetEvent e : events) {
//					StringBuilder sb = new StringBuilder();
//					if (e.causeSnippet() == null) {
//						// We have a snippet creation event
//						switch (e.status()) {
//						case VALID:
//							sb.append("Successful ");
//							break;
//						case RECOVERABLE_DEFINED:
//							sb.append("With unresolved references ");
//							break;
//						case RECOVERABLE_NOT_DEFINED:
//							sb.append("Possibly reparable, failed  ");
//							break;
//						case REJECTED:
//							sb.append("Failed ");
//							break;
//						}
//						if (e.previousStatus() == Status.NONEXISTENT) {
//							sb.append("addition");
//						} else {
//							sb.append("modification");
//						}
//						sb.append(" of ");
//						sb.append(e.snippet().source());
//						System.out.println(sb);
//						if (e.value() != null) {
//							System.out.printf("Value is: %s\n", e.value());
//						}
//						System.out.flush();
//					}
//				}
//			});
////			} while (true);
//		}
//	}

	public void stop() {
		try {
			Files.delete(linkedDir);
		} catch (IOException e) {
			log.error("Cannot remove " + linkedDir);
		}
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

//	public static void main(String[] args) throws Exception {
//		Pipe inPipe = Pipe.open();
//		Pipe outPipe = Pipe.open();
//
//		InputStream in = Channels.newInputStream(inPipe.source());
//		OutputStream out = Channels.newOutputStream(outPipe.sink());
//		JavaShellToolBuilder builder = JavaShellToolBuilder.builder();
//		builder.in(in, null);
//		builder.interactiveTerminal(true);
//		builder.out(new PrintStream(out));
//
//		UnixDomainSocketAddress ioSocketAddress = JShellClient.ioSocketAddress();
//		Files.deleteIfExists(ioSocketAddress.getPath());
//
//		try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
//			serverChannel.bind(ioSocketAddress);
//
//			try (SocketChannel channel = serverChannel.accept()) {
//				new Thread(() -> {
//
//					try {
//						ByteBuffer buffer = ByteBuffer.allocate(1024);
//						while (true) {
//							if (channel.read(buffer) < 0)
//								break;
//							buffer.flip();
//							inPipe.sink().write(buffer);
//							buffer.rewind();
//						}
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}, "Read in").start();
//
//				new Thread(() -> {
//
//					try {
//						ByteBuffer buffer = ByteBuffer.allocate(1024);
//						while (true) {
//							if (outPipe.source().read(buffer) < 0)
//								break;
//							buffer.flip();
//							channel.write(buffer);
//							buffer.rewind();
//						}
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}, "Write out").start();
//
//				builder.start();
//			}
//		} finally {
//			System.out.println("Completed");
//		}
//	}

}
