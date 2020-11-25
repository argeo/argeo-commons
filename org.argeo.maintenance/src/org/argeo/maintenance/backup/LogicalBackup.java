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
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
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
	public final static String OSGI_BASE = "share/osgi/";
	private final Repository repository;
	private final BundleContext bundleContext;

	private final ZipOutputStream zout;
	private final Path basePath;

	public LogicalBackup(BundleContext bundleContext, Repository repository, Path basePath) {
		this.repository = repository;
		this.zout = null;
		this.basePath = basePath;
		this.bundleContext = bundleContext;
	}

//	public LogicalBackup(BundleContext bundleContext, Repository repository, ZipOutputStream zout) {
//	this.repository = repository;
//	this.zout = zout;
//	this.basePath = null;
//	this.bundleContext = bundleContext;
//}

	@Override
	public void run() {
		try {
			log.info("Start logical backup to " + basePath);
			perform();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Logical backup failed", e);
		}

	}

	public void perform() throws RepositoryException, IOException {
		// software backup
		if (bundleContext != null)
			performSoftwareBackup();

		// data backup
		Session defaultSession = login(null);
		try {
			String[] workspaceNames = defaultSession.getWorkspace().getAccessibleWorkspaceNames();
			workspaces: for (String workspaceName : workspaceNames) {
				if ("security".equals(workspaceName))
					continue workspaces;
				perform(workspaceName);
			}
		} finally {
			JcrUtils.logoutQuietly(defaultSession);
		}

	}

	public void performSoftwareBackup() throws IOException {
		for (Bundle bundle : bundleContext.getBundles()) {
			String relativePath = OSGI_BASE + "boot/" + bundle.getSymbolicName() + ".jar";
			Dictionary<String, String> headers = bundle.getHeaders();
			Manifest manifest = new Manifest();
			Enumeration<String> headerKeys = headers.keys();
			while (headerKeys.hasMoreElements()) {
				String headerKey = headerKeys.nextElement();
				String headerValue = headers.get(headerKey);
				manifest.getMainAttributes().putValue(headerKey, headerValue);
			}
			try (JarOutputStream jarOut = new JarOutputStream(openOutputStream(relativePath), manifest)) {
//				Enumeration<String> entryPaths = bundle.getEntryPaths("/");
//				while (entryPaths.hasMoreElements()) {
//					String entryPath = entryPaths.nextElement();
//					ZipEntry entry = new ZipEntry(entryPath);
//					URL entryUrl = bundle.getEntry(entryPath);
//					try (InputStream in = entryUrl.openStream()) {
//						jarOut.putNextEntry(entry);
//						IOUtils.copy(in, jarOut);
//						jarOut.closeEntry();
//					} catch (FileNotFoundException e) {
//						log.warn(entryPath);
//					}
//				}
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
			}
		}

	}

	public void perform(String workspaceName) throws RepositoryException, IOException {
		Session session = login(workspaceName);
		try {
			String relativePath = WORKSPACES_BASE + workspaceName + ".xml";
			OutputStream xmlOut = openOutputStream(relativePath);
			BackupContentHandler contentHandler;
			try (Writer writer = new BufferedWriter(new OutputStreamWriter(xmlOut, StandardCharsets.UTF_8))) {
				contentHandler = new BackupContentHandler(writer, session);
				try {
					session.exportSystemView("/", contentHandler, true, false);
					if (log.isDebugEnabled())
						log.debug("Workspace " + workspaceName + ": metadata exported to " + relativePath);
				} catch (PathNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RepositoryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for (String path : contentHandler.getContentPaths()) {
				Node contentNode = session.getNode(path);
				Binary binary = contentNode.getProperty(Property.JCR_DATA).getBinary();
				String fileRelativePath = WORKSPACES_BASE + workspaceName + contentNode.getParent().getPath();
				try (InputStream in = binary.getStream(); OutputStream out = openOutputStream(fileRelativePath)) {
					IOUtils.copy(in, out);
					if (log.isDebugEnabled())
						log.debug("Workspace " + workspaceName + ": file content exported to " + fileRelativePath);
				} finally {

				}

			}

//			OutputStream xmlOut = openOutputStream(relativePath);
//			try {
//				session.exportSystemView("/", xmlOut, false, false);
//			} finally {
//				closeOutputStream(relativePath, xmlOut);
//			}

			// TODO scan all binaries
		} finally {
			JcrUtils.logoutQuietly(session);
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

}
