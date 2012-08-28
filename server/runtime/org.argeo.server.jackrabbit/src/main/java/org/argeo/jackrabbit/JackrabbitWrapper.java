/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;

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
import org.argeo.jcr.JcrRepositoryWrapper;
import org.argeo.jcr.JcrUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Wrapper around a Jackrabbit repository which allows to simplify configuration
 * and intercept some actions. It exposes itself as a {@link Repository}.
 */
public class JackrabbitWrapper extends JcrRepositoryWrapper implements
		ResourceLoaderAware {
	private Log log = LogFactory.getLog(JackrabbitWrapper.class);

	// local
	private ResourceLoader resourceLoader;

	// data model
	/** Node type definitions in CND format */
	private List<String> cndFiles = new ArrayList<String>();
	/**
	 * Always import CNDs. Useful during development of new data models. In
	 * production, explicit migration processes should be used.
	 */
	private Boolean forceCndImport = false;

	/** Namespaces to register: key is prefix, value namespace */
	private Map<String, String> namespaces = new HashMap<String, String>();

	private BundleContext bundleContext;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JackrabbitWrapper() {
	}

	@Override
	public void init() {
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
		if ((cndFiles == null || cndFiles.size() == 0)
				&& (namespaces == null || namespaces.size() == 0))
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
						// if (url == null)
						// url = Thread.currentThread()
						// .getContextClassLoader()
						// .getResource(resUrl);
					}
				} else {
					url = new URL(resUrl);
				}

				// check existing data model nodes
				new NamespaceHelper(session).registerNamespace(
						ArgeoNames.ARGEO, ArgeoNames.ARGEO_NAMESPACE);
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

				InputStream in = null;
				Reader reader = null;
				try {
					if (url != null) {
						in = url.openStream();
					} else if (resourceLoader != null) {
						Resource res = resourceLoader.getResource(resUrl);
						in = res.getInputStream();
						url = res.getURL();
					} else {
						throw new ArgeoException("No " + resUrl
								+ " in the classpath,"
								+ " make sure the containing"
								+ " package is visible.");
					}

					reader = new InputStreamReader(in);
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
					IOUtils.closeQuietly(in);
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

	/*
	 * REPOSITORY INTERCEPTOR
	 */

	/*
	 * UTILITIES
	 */
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
	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}

	public void setCndFiles(List<String> cndFiles) {
		this.cndFiles = cndFiles;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setForceCndImport(Boolean forceCndUpdate) {
		this.forceCndImport = forceCndUpdate;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
