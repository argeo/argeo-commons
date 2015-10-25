/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.osgi.boot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

/**
 * A distribution bundle is a bundle within a maven-like distribution
 * groupId:Bundle-SymbolicName:Bundle-Version which references others OSGi
 * bundle. It is not required to be OSGi complete also it will generally be
 * expected that it is. The root of the repository is computed based on the file
 * name of the URL and of the content of the index.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DistributionBundle {
	private final static String INDEX_FILE_NAME = "modularDistribution.csv";

	private final String url;

	private Manifest manifest;
	private String symbolicName;
	private String version;

	/** can be null */
	private String baseUrl;
	/** can be null */
	private String relativeUrl;

	private List/* <OsgiArtifact> */artifacts;

	private String separator = ",";

	public DistributionBundle(String url) {
		this.url = url;
	}

	public DistributionBundle(String baseUrl, String relativeUrl) {
		if (baseUrl == null || !baseUrl.endsWith("/"))
			throw new OsgiBootException("Base url " + baseUrl
					+ " badly formatted");
		if (relativeUrl.startsWith("http") || relativeUrl.startsWith("file:"))
			throw new OsgiBootException("Relative URL " + relativeUrl
					+ " badly formatted");
		this.url = baseUrl + relativeUrl;
		this.baseUrl = baseUrl;
		this.relativeUrl = relativeUrl;
	}

	public void processUrl() {
		JarInputStream jarIn = null;
		try {
			URL u = new URL(url);
			jarIn = new JarInputStream(u.openStream());

			// meta data
			manifest = jarIn.getManifest();
			symbolicName = manifest.getMainAttributes().getValue(
					Constants.BUNDLE_SYMBOLICNAME);
			version = manifest.getMainAttributes().getValue(
					Constants.BUNDLE_VERSION);

			JarEntry indexEntry;
			while ((indexEntry = jarIn.getNextJarEntry()) != null) {
				String entryName = indexEntry.getName();
				if (entryName.equals(INDEX_FILE_NAME)) {
					break;
				}
				jarIn.closeEntry();
			}

			// list artifacts
			if (indexEntry == null)
				throw new OsgiBootException("No index " + INDEX_FILE_NAME
						+ " in " + url);
			artifacts = listArtifacts(jarIn);
			jarIn.closeEntry();

			// find base URL
			// won't work if distribution artifact is not listed
			for (int i = 0; i < artifacts.size(); i++) {
				OsgiArtifact osgiArtifact = (OsgiArtifact) artifacts.get(i);
				if (osgiArtifact.getSymbolicName().equals(symbolicName)
						&& osgiArtifact.getVersion().equals(version)) {
					String relativeUrl = osgiArtifact.getRelativeUrl();
					if (url.endsWith(relativeUrl)) {
						baseUrl = url.substring(0, url.length()
								- osgiArtifact.getRelativeUrl().length());
						break;
					}
				}
			}
		} catch (Exception e) {
			throw new OsgiBootException("Cannot list URLs from " + url, e);
		} finally {
			if (jarIn != null)
				try {
					jarIn.close();
				} catch (IOException e) {
					// silent
				}
		}
	}

	protected List/* <OsgiArtifact> */listArtifacts(InputStream in) {
		List osgiArtifacts = new ArrayList();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, separator);
				String moduleName = st.nextToken();
				String moduleVersion = st.nextToken();
				String relativeUrl = st.nextToken();
				osgiArtifacts.add(new OsgiArtifact(moduleName, moduleVersion,
						relativeUrl));
			}
		} catch (Exception e) {
			throw new OsgiBootException("Cannot list artifacts", e);
		}
		return osgiArtifacts;
	}

	/** Convenience method */
	public static DistributionBundle processUrl(String baseUrl,
			String realtiveUrl) {
		DistributionBundle distributionBundle = new DistributionBundle(baseUrl,
				realtiveUrl);
		distributionBundle.processUrl();
		return distributionBundle;
	}

	/**
	 * List full URLs of the bundles, based on base URL, usable directly for
	 * download.
	 */
	public List<String> listUrls() {
		if (baseUrl == null)
			throw new OsgiBootException("Base URL is not set");

		if (artifacts == null)
			throw new OsgiBootException("Artifact list not initialized");

		List/* <String> */urls = new ArrayList();
		for (int i = 0; i < artifacts.size(); i++) {
			OsgiArtifact osgiArtifact = (OsgiArtifact) artifacts.get(i);
			urls.add(baseUrl + osgiArtifact.getRelativeUrl());
		}
		return urls;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/** Separator used to parse the tabular file */
	public void setSeparator(String modulesUrlSeparator) {
		this.separator = modulesUrlSeparator;
	}

	public String getRelativeUrl() {
		return relativeUrl;
	}

	/** One of the listed artifact */
	protected static class OsgiArtifact {
		private final String symbolicName;
		private final String version;
		private final String relativeUrl;

		public OsgiArtifact(String symbolicName, String version,
				String relativeUrl) {
			super();
			this.symbolicName = symbolicName;
			this.version = version;
			this.relativeUrl = relativeUrl;
		}

		public String getSymbolicName() {
			return symbolicName;
		}

		public String getVersion() {
			return version;
		}

		public String getRelativeUrl() {
			return relativeUrl;
		}

	}
}
