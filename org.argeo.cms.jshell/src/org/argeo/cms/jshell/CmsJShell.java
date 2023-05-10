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

/** A factory for JShell sessions. */
public class CmsJShell {
	private final static CmsLog log = CmsLog.getLog(CmsJShell.class);
	static ClassLoader loader = CmsJShell.class.getClassLoader();

	public static UuidFactory uuidFactory = null;

	private CmsState cmsState;

	private Map<Path, LocalJShellSession> localSessions = new HashMap<>();
	private Map<Path, Path> bundleDirs = new HashMap<>();

	private Path stateRunDir;
	private Path jshBase;
	private Path jshLinkedDir;
	private Path jtermBase;
	private Path jtermLinkedDir;

	public void start() throws Exception {
		// TODO better define application id, make it configurable
		String applicationID = cmsState.getStatePath("").getFileName().toString();

		// TODO centralise state run dir
		stateRunDir = OS.getRunDir().resolve(applicationID);

		jshBase = stateRunDir.resolve(JShellClient.JSH);
		Files.createDirectories(jshBase);
		jshLinkedDir = Files.createSymbolicLink(cmsState.getStatePath(JShellClient.JSH), jshBase);

		jtermBase = stateRunDir.resolve(JShellClient.JTERM);
		Files.createDirectories(jtermBase);
		jtermLinkedDir = Files.createSymbolicLink(cmsState.getStatePath(JShellClient.JTERM), jtermBase);

		log.info("Local JShell on " + jshBase + ", linked to " + jshLinkedDir);
		log.info("Local JTerm on " + jtermBase + ", linked to " + jtermLinkedDir);

		new Thread(() -> {
			try {
				WatchService watchService = FileSystems.getDefault().newWatchService();

				jshBase.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_DELETE);
				try (DirectoryStream<Path> bundleSns = Files.newDirectoryStream(jshBase)) {
					for (Path bundleSnDir : bundleSns) {
						addBundleSnDir(bundleSnDir, watchService);
					}
				}
				jtermBase.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_DELETE);
				try (DirectoryStream<Path> bundleSns = Files.newDirectoryStream(jtermBase)) {
					for (Path bundleSnDir : bundleSns) {
						addBundleSnDir(bundleSnDir, watchService);
					}
				}

				WatchKey key;
				while ((key = watchService.take()) != null) {
					events: for (WatchEvent<?> event : key.pollEvents()) {
//						System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
						Path parent = (Path) key.watchable();
						// sessions
						if (Files.isSameFile(jshBase, parent)) {
							Path bundleSnDir = jshBase.resolve((Path) event.context());
							if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
								addBundleSnDir(bundleSnDir, watchService);
							} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
							}
						} else if (Files.isSameFile(jtermBase, parent)) {
							Path bundleSnDir = jtermBase.resolve((Path) event.context());
							if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
								addBundleSnDir(bundleSnDir, watchService);
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

								boolean interactive;
								if (Files.isSameFile(jshBase, parent.getParent())) {
									interactive = false;
								} else if (Files.isSameFile(jtermBase, parent.getParent())) {
									interactive = true;
								} else {
									log.warn("Ignoring " + path + " as we don't know whether it is interactive or not");
									continue events;
								}
								Path bundleIdDir = bundleDirs.get(parent);
								LocalJShellSession localSession = new LocalJShellSession(path, bundleIdDir,
										interactive);
								localSessions.put(path, localSession);
							} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
								localSessions.remove(path);
							}
						}
					}
					key.reset();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}, "JShell local sessions watcher").start();
	}

	private void addBundleSnDir(Path bundleSnDir, WatchService watchService) throws IOException {
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

		bundleSnDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
	}

	public void stop() {
		try {
			Files.delete(jshLinkedDir);
		} catch (IOException e) {
			log.error("Cannot remove " + jshLinkedDir);
		}
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}
}
