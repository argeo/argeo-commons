package org.argeo.maintenance.backup;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.jackrabbit.client.ClientDavexRepositoryFactory;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

/**
 * Performs a backup of the data based only on programmatic interfaces. Useful
 * for migration or live backup. Physical backups of the underlying file
 * systems, databases, LDAP servers, etc. should be performed for disaster
 * recovery.
 */
public class LogicalBackup implements Runnable {
	private final static Log log = LogFactory.getLog(LogicalBackup.class);

	public final static String WORKSPACES_BASE = "workspaces/";
	public final static String FILES_BASE = "files/";
	public final static String OSGI_BASE = "share/osgi/";
	private final Repository repository;
	private String defaultWorkspace;
	private final BundleContext bundleContext;

	private final ZipOutputStream zout;
	private final Path basePath;

	private ExecutorService executorService;

	public LogicalBackup(BundleContext bundleContext, Repository repository, Path basePath) {
		this.repository = repository;
		this.zout = null;
		this.basePath = basePath;
		this.bundleContext = bundleContext;

		executorService = Executors.newFixedThreadPool(3);
	}

	@Override
	public void run() {
		try {
			log.info("Start logical backup to " + basePath);
			perform();
		} catch (Exception e) {
			log.error("Unexpected exception when performing logical backup", e);
			throw new IllegalStateException("Logical backup failed", e);
		}

	}

	public void perform() throws RepositoryException, IOException {
		long begin = System.currentTimeMillis();
		// software backup
		if (bundleContext != null)
			executorService.submit(() -> performSoftwareBackup(bundleContext));

		// data backup
		Session defaultSession = login(null);
		defaultWorkspace = defaultSession.getWorkspace().getName();
		try {
			String[] workspaceNames = defaultSession.getWorkspace().getAccessibleWorkspaceNames();
			workspaces: for (String workspaceName : workspaceNames) {
				if ("security".equals(workspaceName))
					continue workspaces;
				performDataBackup(workspaceName);
			}
		} finally {
			JcrUtils.logoutQuietly(defaultSession);
			executorService.shutdown();
			try {
				executorService.awaitTermination(24, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				// silent
			}
		}
		long duration = System.currentTimeMillis() - begin;
		log.info("System logical backup completed in " + (duration / 60000) + "min " + (duration / 1000) + "s");
	}

	protected void performDataBackup(String workspaceName) throws RepositoryException, IOException {
		Session session = login(workspaceName);
		try {
			nodes: for (NodeIterator nit = session.getRootNode().getNodes(); nit.hasNext();) {
				Node nodeToExport = nit.nextNode();
				if ("jcr:system".equals(nodeToExport.getName()) && !workspaceName.equals(defaultWorkspace))
					continue nodes;
				String nodePath = nodeToExport.getPath();
				Future<Set<String>> contentPathsFuture = executorService
						.submit(() -> performNodeBackup(workspaceName, nodePath));
				executorService.submit(() -> performFilesBackup(workspaceName, contentPathsFuture));
			}
		} finally {
			Jcr.logout(session);
		}
	}

	protected Set<String> performNodeBackup(String workspaceName, String nodePath) {
		Session session = login(workspaceName);
		try {
			Node nodeToExport = session.getNode(nodePath);
			String nodeName = nodeToExport.getName();
//		if (nodeName.startsWith("jcr:") || nodeName.startsWith("rep:"))
//			continue nodes;
//		// TODO make it more robust / configurable
//		if (nodeName.equals("user"))
//			continue nodes;
			String relativePath = WORKSPACES_BASE + workspaceName + "/" + nodeName + ".xml";
			OutputStream xmlOut = openOutputStream(relativePath);
			BackupContentHandler contentHandler;
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(xmlOut, StandardCharsets.UTF_8))) {
				contentHandler = new BackupContentHandler(writer, session);
				session.exportSystemView(nodeToExport.getPath(), contentHandler, true, false);
				if (log.isDebugEnabled())
					log.debug(workspaceName + ":/" + nodeName + " metadata exported to " + relativePath);
			}

			// Files
			Set<String> contentPaths = contentHandler.getContentPaths();
			return contentPaths;
		} catch (IOException | SAXException e) {
			throw new RuntimeException("Cannot backup node " + workspaceName + ":" + nodePath, e);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot backup node " + workspaceName + ":" + nodePath, e);
		} finally {
			Jcr.logout(session);
		}
	}

	protected void performFilesBackup(String workspaceName, Future<Set<String>> contentPathsFuture) {
		Set<String> contentPaths;
		try {
			contentPaths = contentPathsFuture.get(24, TimeUnit.HOURS);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			throw new RuntimeException("Cannot retrieve content paths for workspace " + workspaceName);
		}
		if (contentPaths == null || contentPaths.size() == 0)
			return;
		Session session = login(workspaceName);
		try {
			String workspacesFilesBasePath = FILES_BASE + workspaceName;
			for (String path : contentPaths) {
				Node contentNode = session.getNode(path);
				Binary binary = contentNode.getProperty(Property.JCR_DATA).getBinary();
				String fileRelativePath = workspacesFilesBasePath + contentNode.getParent().getPath();
				try (InputStream in = binary.getStream(); OutputStream out = openOutputStream(fileRelativePath)) {
					IOUtils.copy(in, out);
					if (log.isTraceEnabled())
						log.trace("Workspace " + workspaceName + ": file content exported to " + fileRelativePath);
				} finally {
					JcrUtils.closeQuietly(binary);
				}
			}
			if (log.isDebugEnabled())
				log.debug(workspaceName + ":" + contentPaths.size() + " files exported to " + workspacesFilesBasePath);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot backup files from " + workspaceName + ":", e);
		} catch (IOException e) {
			throw new RuntimeException("Cannot backup files from " + workspaceName + ":", e);
		} finally {
			Jcr.logout(session);
		}
	}

	protected OutputStream openOutputStream(String relativePath) throws IOException {
		if (zout != null) {
			ZipEntry entry = new ZipEntry(relativePath);
			zout.putNextEntry(entry);
			return zout;
		} else if (basePath != null) {
			Path targetPath = basePath.resolve(Paths.get(relativePath));
			Files.createDirectories(targetPath.getParent());
			return Files.newOutputStream(targetPath);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	protected void closeOutputStream(String relativePath, OutputStream out) throws IOException {
		if (zout != null) {
			zout.closeEntry();
		} else if (basePath != null) {
			out.close();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	protected Session login(String workspaceName) {
		if (bundleContext != null) {// local
			return NodeUtils.openDataAdminSession(repository, workspaceName);
		} else {// remote
			try {
				return repository.login(workspaceName);
			} catch (RepositoryException e) {
				throw new JcrException(e);
			}
		}
	}

	public final static void main(String[] args) throws Exception {
		if (args.length == 0) {
			printUsage("No argument");
			System.exit(1);
		}
		URI uri = new URI(args[0]);
		Repository repository = createRemoteRepository(uri);
		Path basePath = args.length > 1 ? Paths.get(args[1]) : Paths.get(System.getProperty("user.dir"));
		if (!Files.exists(basePath))
			Files.createDirectories(basePath);
		LogicalBackup backup = new LogicalBackup(null, repository, basePath);
		backup.run();
	}

	private static void printUsage(String errorMessage) {
		if (errorMessage != null)
			System.err.println(errorMessage);
		System.out.println("Usage: LogicalBackup <remote URL> [<target directory>]");

	}

	protected static Repository createRemoteRepository(URI uri) throws RepositoryException {
		RepositoryFactory repositoryFactory = new ClientDavexRepositoryFactory();
		Map<String, String> params = new HashMap<String, String>();
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_DAVEX_URI, uri.toString());
		// TODO make it configurable
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, NodeConstants.SYS_WORKSPACE);
		return repositoryFactory.getRepository(params);
	}

	public void performSoftwareBackup(BundleContext bundleContext) {
		String bootBasePath = OSGI_BASE + "boot";
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			String relativePath = bootBasePath + "/" + bundle.getSymbolicName() + ".jar";
			Dictionary<String, String> headers = bundle.getHeaders();
			Manifest manifest = new Manifest();
			Enumeration<String> headerKeys = headers.keys();
			while (headerKeys.hasMoreElements()) {
				String headerKey = headerKeys.nextElement();
				String headerValue = headers.get(headerKey);
				manifest.getMainAttributes().putValue(headerKey, headerValue);
			}
			try (JarOutputStream jarOut = new JarOutputStream(openOutputStream(relativePath), manifest)) {
				Enumeration<URL> resourcePaths = bundle.findEntries("/", "*", true);
				resources: while (resourcePaths.hasMoreElements()) {
					URL entryUrl = resourcePaths.nextElement();
					String entryPath = entryUrl.getPath();
					if (entryPath.equals(""))
						continue resources;
					if (entryPath.endsWith("/"))
						continue resources;
					String entryName = entryPath.substring(1);// remove first '/'
					if (entryUrl.getPath().equals("/META-INF/"))
						continue resources;
					if (entryUrl.getPath().equals("/META-INF/MANIFEST.MF"))
						continue resources;
					// dev
					if (entryUrl.getPath().startsWith("/target"))
						continue resources;
					if (entryUrl.getPath().startsWith("/src"))
						continue resources;
					if (entryUrl.getPath().startsWith("/ext"))
						continue resources;

					if (entryName.startsWith("bin/")) {// dev
						entryName = entryName.substring("bin/".length());
					}

					ZipEntry entry = new ZipEntry(entryName);
					try (InputStream in = entryUrl.openStream()) {
						try {
							jarOut.putNextEntry(entry);
						} catch (ZipException e) {// duplicate
							continue resources;
						}
						IOUtils.copy(in, jarOut);
						jarOut.closeEntry();
//						log.info(entryUrl);
					} catch (FileNotFoundException e) {
						log.warn(entryUrl + ": " + e.getMessage());
					}
				}
			} catch (IOException e1) {
				throw new RuntimeException("Cannot export bundle " + bundle, e1);
			}
		}
		if (log.isDebugEnabled())
			log.debug(bundles.length + " OSGi bundles exported to " + bootBasePath);

	}

}
