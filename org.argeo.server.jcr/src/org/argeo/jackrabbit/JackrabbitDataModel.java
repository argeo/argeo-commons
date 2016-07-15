package org.argeo.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.util.security.DigestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class JackrabbitDataModel {
	private final static Log log = LogFactory.getLog(JackrabbitDataModel.class);
	private final static String DIGEST_ALGORITHM = "MD5";
	final static String[] DEFAULT_CNDS = { "/org/argeo/jcr/argeo.cnd", "/org/argeo/cms/cms.cnd", "/org/argeo/jcr/docbook/docbook.cnd" };

	// data model
	/** Node type definitions in CND format */
	private List<String> cndFiles = new ArrayList<String>();
	/**
	 * Always import CNDs. Useful during development of new data models. In
	 * production, explicit migration processes should be used.
	 */
	private Boolean forceCndImport = true;

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private final BundleContext bc;

	public JackrabbitDataModel(BundleContext bc) {
		this.bc = bc;
	}

	/**
	 * Import declared node type definitions and register namespaces. Tries to
	 * update the node definitions if they have changed. In case of failures an
	 * error will be logged but no exception will be thrown.
	 */
	public void prepareDataModel(Repository repository) {
		cndFiles = Arrays.asList(DEFAULT_CNDS);
		if ((cndFiles == null || cndFiles.size() == 0) && (namespaces == null || namespaces.size() == 0))
			return;

		Session session = null;
		try {
			session = repository.login();
			// register namespaces
			if (namespaces.size() > 0) {
				NamespaceHelper namespaceHelper = new NamespaceHelper(session);
				namespaceHelper.registerNamespaces(namespaces);
			}

			// load CND files from classpath or as URL
			for (String resUrl : cndFiles) {
				processCndFile(session, resUrl);
			}
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot import node type definitions " + cndFiles, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}

	}

	protected void processCndFile(Session session, String resUrl) {
		Reader reader = null;
		try {
			// check existing data model nodes
			new NamespaceHelper(session).registerNamespace(ArgeoNames.ARGEO, ArgeoNames.ARGEO_NAMESPACE);
			if (!session.itemExists(ArgeoJcrConstants.DATA_MODELS_BASE_PATH))
				JcrUtils.mkdirs(session, ArgeoJcrConstants.DATA_MODELS_BASE_PATH);
			Node dataModels = session.getNode(ArgeoJcrConstants.DATA_MODELS_BASE_PATH);
			NodeIterator it = dataModels.getNodes();
			Node dataModel = null;
			while (it.hasNext()) {
				Node node = it.nextNode();
				if (node.getProperty(ArgeoNames.ARGEO_URI).getString().equals(resUrl)) {
					dataModel = node;
					break;
				}
			}

			Bundle bundle = findDataModelBundle(resUrl);

			byte[] cndContent = readCndContent(resUrl);
			String newDigest = DigestUtils.digest(DIGEST_ALGORITHM, cndContent);

			String currentVersion = null;
			if (dataModel != null) {
				currentVersion = dataModel.getProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION).getString();
				if (dataModel.hasNode(Node.JCR_CONTENT)) {
					String oldDigest = JcrUtils.checksumFile(dataModel, DIGEST_ALGORITHM);
					if (oldDigest.equals(newDigest)) {
						if (log.isTraceEnabled())
							log.trace("Data model " + resUrl + " hasn't changed, keeping version " + currentVersion);
						return;
					}
				}
			}

			if (dataModel != null && !forceCndImport) {
				log.info(
						"Data model " + resUrl + " has changed since version " + currentVersion
								+ (bundle != null
										? ": version " + bundle.getVersion() + ", bundle " + bundle.getSymbolicName()
										: ""));
				return;
			}

			reader = new InputStreamReader(new ByteArrayInputStream(cndContent));
			// actually imports the CND
			try {
				CndImporter.registerNodeTypes(reader, session, true);
			} catch (Exception e) {
				log.error("Cannot import data model " + resUrl, e);
				return;
			}

			if (dataModel != null && !dataModel.isNodeType(NodeType.NT_FILE)) {
				dataModel.remove();
				dataModel = null;
			}

			// FIXME: what if argeo.cnd would not be the first called on
			// a new repo? argeo:dataModel would not be found
			String fileName = FilenameUtils.getName(resUrl);
			if (dataModel == null) {
				dataModel = dataModels.addNode(fileName, NodeType.NT_FILE);
				dataModel.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);
				dataModel.addMixin(ArgeoTypes.ARGEO_DATA_MODEL);
				dataModel.setProperty(ArgeoNames.ARGEO_URI, resUrl);
			} else {
				session.getWorkspace().getVersionManager().checkout(dataModel.getPath());
			}
			if (bundle != null)
				dataModel.setProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION, bundle.getVersion().toString());
			else
				dataModel.setProperty(ArgeoNames.ARGEO_DATA_MODEL_VERSION, "0.0.0");
			JcrUtils.copyBytesAsFile(dataModel.getParent(), fileName, cndContent);
			JcrUtils.updateLastModified(dataModel);
			session.save();
			session.getWorkspace().getVersionManager().checkin(dataModel.getPath());

			if (currentVersion == null)
				log.info(
						"Data model " + resUrl
								+ (bundle != null
										? ", version " + bundle.getVersion() + ", bundle " + bundle.getSymbolicName()
										: ""));
			else
				log.info(
						"Data model " + resUrl + " updated from version " + currentVersion
								+ (bundle != null
										? ", version " + bundle.getVersion() + ", bundle " + bundle.getSymbolicName()
										: ""));
		} catch (Exception e) {
			throw new ArgeoException("Cannot process data model " + resUrl, e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	protected byte[] readCndContent(String resUrl) {
		BundleContext bundleContext = bc;
		InputStream in = null;
		try {
			boolean classpath;
			// normalize URL
			if (bundleContext != null && resUrl.startsWith("classpath:")) {
				resUrl = resUrl.substring("classpath:".length());
				classpath = true;
			} else if (resUrl.indexOf(':') < 0) {
				if (!resUrl.startsWith("/")) {
					resUrl = "/" + resUrl;
					log.warn("Classpath should start with '/'");
				}
				classpath = true;
			} else {
				classpath = false;
			}

			URL url = null;
			if (classpath) {
				// if (bundleContext != null) {
				Bundle currentBundle = bundleContext.getBundle();
				url = currentBundle.getResource(resUrl);
				// } else {
				// resUrl = "classpath:" + resUrl;
				// url = null;
				// }
			} else if (!resUrl.startsWith("classpath:")) {
				url = new URL(resUrl);
			}

			if (url != null) {
				in = url.openStream();
				// } else if (resourceLoader != null) {
				// Resource res = resourceLoader.getResource(resUrl);
				// in = res.getInputStream();
				// url = res.getURL();
			} else {
				throw new ArgeoException(
						"No " + resUrl + " in the classpath," + " make sure the containing" + " package is visible.");
			}

			return IOUtils.toByteArray(in);
		} catch (Exception e) {
			throw new ArgeoException("Cannot read CND from " + resUrl, e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	/*
	 * UTILITIES
	 */
	/** Find which OSGi bundle provided the data model resource */
	protected Bundle findDataModelBundle(String resUrl) {
		BundleContext bundleContext = bc;
		if (bundleContext == null)
			return null;

		if (resUrl.startsWith("/"))
			resUrl = resUrl.substring(1);
		String pkg = resUrl.substring(0, resUrl.lastIndexOf('/')).replace('/', '.');
		ServiceReference<PackageAdmin> paSr = bundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(paSr);

		// find exported package
		ExportedPackage exportedPackage = null;
		ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(pkg);
		if (exportedPackages == null)
			throw new ArgeoException("No exported package found for " + pkg);
		for (ExportedPackage ep : exportedPackages) {
			for (Bundle b : ep.getImportingBundles()) {
				if (b.getBundleId() == bundleContext.getBundle().getBundleId()) {
					exportedPackage = ep;
					break;
				}
			}
		}

		Bundle exportingBundle = null;
		if (exportedPackage != null) {
			exportingBundle = exportedPackage.getExportingBundle();
		} else {
			// assume this is in the same bundle
			exportingBundle = bundleContext.getBundle();
			// throw new ArgeoException("No OSGi exporting package found for "
			// + resUrl);
		}
		return exportingBundle;
	}

}
