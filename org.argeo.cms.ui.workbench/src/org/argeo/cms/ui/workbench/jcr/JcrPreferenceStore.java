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
package org.argeo.cms.ui.workbench.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.jcr.JcrUtils;
import org.argeo.node.ArgeoNames;
import org.argeo.node.ArgeoTypes;
import org.argeo.node.NodeUtils;
import org.eclipse.jface.preference.PreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * Persist preferences as key/value pairs under ~/argeo:preferences.<br>
 * TODO: better integrate JCR and Eclipse:<br>
 * - typing<br>
 * - use eclipse preferences<br>
 * - better integrate with <code>ScopedPreferenceStore</code> provided by RAP
 */
public class JcrPreferenceStore extends PreferenceStore implements ArgeoNames {
	private static final long serialVersionUID = 1854011367784598758L;

	private Session session;
	private BundleContext bundleContext;

	/** Retrieves the preference node */
	protected Node getPreferenceNode() {
		try {
			if (session.hasPendingChanges())
				session.save();
			Node userHome = NodeUtils.getUserHome(session);
			if (userHome == null)
				throw new EclipseUiException("No user home for "
						+ session.getUserID());
			Node preferences;
			if (!userHome.hasNode(ARGEO_PREFERENCES)) {
				preferences = userHome.addNode(ARGEO_PREFERENCES);
				preferences.addMixin(ArgeoTypes.ARGEO_PREFERENCE_NODE);
				session.save();
			} else
				preferences = userHome.getNode(ARGEO_PREFERENCES);

			String pluginPreferencesName = bundleContext.getBundle()
					.getSymbolicName();
			Node pluginPreferences;
			if (!preferences.hasNode(pluginPreferencesName)) {
				VersionManager vm = session.getWorkspace().getVersionManager();
				vm.checkout(preferences.getPath());
				pluginPreferences = preferences.addNode(pluginPreferencesName);
				pluginPreferences.addMixin(ArgeoTypes.ARGEO_PREFERENCE_NODE);
				session.save();
				vm.checkin(preferences.getPath());
			} else
				pluginPreferences = preferences.getNode(pluginPreferencesName);
			return pluginPreferences;
		} catch (RepositoryException e) {
			e.printStackTrace();
			JcrUtils.discardQuietly(session);
			throw new EclipseUiException("Cannot retrieve preferences", e);
		}

	}

	@Override
	public void load() throws IOException {
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		try {
			Properties props = new Properties();
			PropertyIterator it = getPreferenceNode().getProperties();
			while (it.hasNext()) {
				Property p = it.nextProperty();
				if (!p.isMultiple() && !p.getDefinition().isProtected()) {
					props.setProperty(p.getName(), p.getValue().getString());
				}
			}
			out = new ByteArrayOutputStream();
			props.store(out, "");
			in = new ByteArrayInputStream(out.toByteArray());
			load(in);
		} catch (Exception e) {
			e.printStackTrace();
			throw new EclipseUiException("Cannot load preferences", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	public void save() throws IOException {
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		Node pluginPreferences = null;
		try {
			out = new ByteArrayOutputStream();
			save(out, "");
			in = new ByteArrayInputStream(out.toByteArray());
			Properties props = new Properties();
			props.load(in);
			pluginPreferences = getPreferenceNode();
			VersionManager vm = pluginPreferences.getSession().getWorkspace()
					.getVersionManager();
			vm.checkout(pluginPreferences.getPath());
			for (Object key : props.keySet()) {
				String name = key.toString();
				String value = props.getProperty(name);
				pluginPreferences.setProperty(name, value);
			}
			JcrUtils.updateLastModified(pluginPreferences);
			pluginPreferences.getSession().save();
			vm.checkin(pluginPreferences.getPath());
		} catch (Exception e) {
			JcrUtils.discardUnderlyingSessionQuietly(pluginPreferences);
			throw new EclipseUiException("Cannot save preferences", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	public void init() {
		try {
			load();
		} catch (IOException e) {
			throw new EclipseUiException("Cannot initialize preference store", e);
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}