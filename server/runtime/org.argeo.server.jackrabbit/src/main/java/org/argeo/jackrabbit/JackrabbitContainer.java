/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.jackrabbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.ArgeoTypes;
import org.argeo.jcr.JcrUtils;
import org.argeo.security.SystemAuthentication;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.core.io.Resource;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.util.SystemPropertyUtils;

/**
 * Wrapper around a Jackrabbit repository which allows to configure it in Spring
 * and expose it as a {@link Repository}.
 */
public class JackrabbitContainer extends JackrabbitWrapper {
	private Log log = LogFactory.getLog(JackrabbitContainer.class);

	// remote
	private Credentials remoteSystemCredentials = null;

	// local
	private Resource configuration;
	private Resource variables;

	// data model
	/** Node type definitions in CND format */
	private List<String> cndFiles = new ArrayList<String>();
	/**
	 * Always import CNDs. Useful during development of new data models. In
	 * production, explicit migration processes should be used.
	 */
	private Boolean forceCndImport = false;

	/** Migrations to execute (if not already done) */
	private Set<JackrabbitDataModelMigration> dataModelMigrations = new HashSet<JackrabbitDataModelMigration>();

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private BundleContext bundleContext;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JackrabbitContainer() {
	}

	/**
	 * Convenience constructor for remote, {@link #init()} is called in the
	 * constructor.
	 */
	public JackrabbitContainer(String uri, Credentials remoteSystemCredentials) {
		setUri(uri);
		setRemoteSystemCredentials(remoteSystemCredentials);
		init();
	}

	@Override
	protected void postInitWrapped() {
		prepareDataModel();
	}

	@Override
	protected void postInitNew() {
		// migrate if needed
		migrate();

		// apply new CND files after migration
		if (cndFiles != null && cndFiles.size() > 0)
			prepareDataModel();
	}

	/*
	 * DATA MODEL
	 */

	/**
	 * Import declared node type definitions and register namespaces. Tries to
	 * update the node definitions if they have changed. In case of failures an
	 * error will be logged but no exception will be thrown.
	 */
	protected void prepareDataModel() {
		// importing node def on remote si currently not supported
		if (isRemote())
			return;

		Session session = null;
		try {
			session = login();
			// register namespaces
			if (namespaces.size() > 0) {
				NamespaceHelper namespaceHelper = new NamespaceHelper(session);
				namespaceHelper.registerNamespaces(namespaces);
			}
			// load CND files from classpath or as URL
			for (String resUrl : cndFiles) {
				boolean classpath;
				// normalize URL
				if (resUrl.startsWith("classpath:")) {
					resUrl = resUrl.substring("classpath:".length());
					classpath = true;
				} else if (resUrl.indexOf(':') < 0) {
					if (!resUrl.startsWith("/")) {
						resUrl = "/" + resUrl;
						log.warn("Classpath should start with '/'");
					}
					// resUrl = "classpath:" + resUrl;
					classpath = true;
				} else {
					classpath = false;
				}

				URL url;
				Bundle dataModelBundle = null;
				if (classpath) {
					if (bundleContext != null) {
						Bundle currentBundle = bundleContext.getBundle();
						url = currentBundle.getResource(resUrl);
						if (url != null) {// found
							dataModelBundle = findDataModelBundle(resUrl);
						}
					} else {
						url = getClass().getClassLoader().getResource(resUrl);
					}
					if (url == null)
						throw new ArgeoException("No " + resUrl
								+ " in the classpath,"
								+ " make sure the containing"
								+ " package is visible.");

				} else {
					url = new URL(resUrl);
				}

				// check existing data model nodes
				if (!session
						.itemExists(ArgeoJcrConstants.DATA_MODELS_BASE_PATH))
					JcrUtils.mkdirs(session,
							ArgeoJcrConstants.DATA_MODELS_BASE_PATH);
				Node dataModels = session
						.getNode(ArgeoJcrConstants.DATA_MODELS_BASE_PATH);
				NodeIterator it = dataModels.getNodes();
				Node dataModel = null;
				while (it.hasNext()) {
					Node node = it.nextNode();
					if (node.getProperty(ArgeoNames.ARGEO_URI).getString()
							.equals(resUrl)) {
						dataModel = node;
						break;
					}
				}

				// does nothing if data model already registered
				if (dataModel != null && !forceCndImport) {
					if (dataModelBundle != null) {
						String version = dataModel.getProperty(
								ArgeoNames.ARGEO_DATA_MODEL_VERSION)
								.getString();
						String dataModelBundleVersion = dataModelBundle
								.getVersion().toString();
						if (!version.equals(dataModelBundleVersion)) {
							log.warn("Data model with version "
									+ dataModelBundleVersion
									+ " available, current version is "
									+ version);
						}
					}
					// do not implicitly update
					return;
				}

				Reader reader = null;
				try {
					reader = new InputStreamReader(url.openStream());
					// actually imports the CND
					CndImporter.registerNodeTypes(reader, session, true);

					// FIXME: what if argeo.cnd would not be the first called on
					// a new repo? argeo:dataModel would not be found
					String fileName = FilenameUtils.getName(url.getPath());
					if (dataModel == null) {
						dataModel = dataModels.addNode(fileName);
						dataModel.addMixin(ArgeoTypes.ARGEO_DATA_MODEL);
						dataModel.setProperty(ArgeoNames.ARGEO_URI, resUrl);
					} else {
						session.getWorkspace().getVersionManager()
								.checkout(dataModel.getPath());
					}
					if (dataModelBundle != null)
						dataModel.setProperty(
								ArgeoNames.ARGEO_DATA_MODEL_VERSION,
								dataModelBundle.getVersion().toString());
					else
						dataModel.setProperty(
								ArgeoNames.ARGEO_DATA_MODEL_VERSION, "0.0.0");
					JcrUtils.updateLastModified(dataModel);
					session.save();
					session.getWorkspace().getVersionManager()
							.checkin(dataModel.getPath());
				} finally {
					IOUtils.closeQuietly(reader);
				}

				if (log.isDebugEnabled())
					log.debug("Data model "
							+ resUrl
							+ (dataModelBundle != null ? ", version "
									+ dataModelBundle.getVersion()
									+ ", bundle "
									+ dataModelBundle.getSymbolicName() : ""));
			}
		} catch (Exception e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot import node type definitions "
					+ cndFiles, e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}

	}

	/** Executes migrations, if needed. */
	protected void migrate() {
		// Remote migration not supported
		if (isRemote())
			return;

		// No migration to perform
		if (dataModelMigrations.size() == 0)
			return;

		Boolean restartAndClearCaches = false;

		// migrate data
		Session session = null;
		try {
			session = login();
			for (JackrabbitDataModelMigration dataModelMigration : new TreeSet<JackrabbitDataModelMigration>(
					dataModelMigrations)) {
				if (dataModelMigration.migrate(session)) {
					restartAndClearCaches = true;
				}
			}
		} catch (ArgeoException e) {
			throw e;
		} catch (Exception e) {
			throw new ArgeoException("Cannot migrate", e);
		} finally {
			JcrUtils.logoutQuietly(session);
		}

		// restart repository
		if (restartAndClearCaches) {
			Repository repository = getRepository();
			if (repository instanceof RepositoryImpl) {
				JackrabbitDataModelMigration
						.clearRepositoryCaches(((RepositoryImpl) repository)
								.getConfig());
			}
			((JackrabbitRepository) repository).shutdown();
			createJackrabbitRepository();
		}

		// set data model version
		try {
			session = login();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot login to migrated repository", e);
		}

		for (JackrabbitDataModelMigration dataModelMigration : new TreeSet<JackrabbitDataModelMigration>(
				dataModelMigrations)) {
			try {
				if (session.itemExists(dataModelMigration
						.getDataModelNodePath())) {
					Node dataModelNode = session.getNode(dataModelMigration
							.getDataModelNodePath());
					dataModelNode.setProperty(
							ArgeoNames.ARGEO_DATA_MODEL_VERSION,
							dataModelMigration.getTargetVersion());
					session.save();
				}
			} catch (Exception e) {
				log.error("Cannot set model version", e);
			}
		}
		JcrUtils.logoutQuietly(session);

	}

	/*
	 * REPOSITORY INTERCEPTOR
	 */
	/** Central login method */
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {

		// retrieve credentials for remote
		if (credentials == null && isRemote()) {
			Authentication authentication = SecurityContextHolder.getContext()
					.getAuthentication();
			if (authentication != null) {
				if (authentication instanceof UsernamePasswordAuthenticationToken) {
					UsernamePasswordAuthenticationToken upat = (UsernamePasswordAuthenticationToken) authentication;
					credentials = new SimpleCredentials(upat.getName(), upat
							.getCredentials().toString().toCharArray());
				} else if ((authentication instanceof SystemAuthentication)
						&& remoteSystemCredentials != null) {
					credentials = remoteSystemCredentials;
				}
			}
		}

		return super.login(credentials, workspaceName);
	}

	/*
	 * UTILITIES
	 */

	@Override
	protected InputStream readConfiguration() {
		try {
			return configuration != null ? configuration.getInputStream()
					: null;
		} catch (IOException e) {
			throw new ArgeoException("Cannot read Jackrabbit configuration "
					+ configuration, e);
		}
	}

	@Override
	protected InputStream readVariables() {
		try {
			return variables != null ? variables.getInputStream() : null;
		} catch (IOException e) {
			throw new ArgeoException("Cannot read Jackrabbit variables "
					+ variables, e);
		}
	}

	@Override
	protected String resolvePlaceholders(String string,
			Map<String, String> variables) {
		return SystemPropertyUtils.resolvePlaceholders(string);
	}

	/** Find which OSGi bundle provided the data model resource */
	protected Bundle findDataModelBundle(String resUrl) {
		if (resUrl.startsWith("/"))
			resUrl = resUrl.substring(1);
		String pkg = resUrl.substring(0, resUrl.lastIndexOf('/')).replace('/',
				'.');
		ServiceReference paSr = bundleContext
				.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) bundleContext
				.getService(paSr);

		// find exported package
		ExportedPackage exportedPackage = null;
		ExportedPackage[] exportedPackages = packageAdmin
				.getExportedPackages(pkg);
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
			throw new ArgeoException("No OSGi exporting package found for "
					+ resUrl);
		}
		return exportingBundle;
	}

	/*
	 * FIELDS ACCESS
	 */
	public void setConfiguration(Resource configuration) {
		this.configuration = configuration;
	}

	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}

	public void setCndFiles(List<String> cndFiles) {
		this.cndFiles = cndFiles;
	}

	public void setVariables(Resource variables) {
		this.variables = variables;
	}

	public void setRemoteSystemCredentials(Credentials remoteSystemCredentials) {
		this.remoteSystemCredentials = remoteSystemCredentials;
	}

	public void setDataModelMigrations(
			Set<JackrabbitDataModelMigration> dataModelMigrations) {
		this.dataModelMigrations = dataModelMigrations;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setForceCndImport(Boolean forceCndUpdate) {
		this.forceCndImport = forceCndUpdate;
	}

}
