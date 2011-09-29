package org.argeo.eclipse.ui.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.eclipse.jface.preference.PreferenceStore;

/** Persist preferences as key/value pairs under ~/argeo:preferences */
public class JcrPreferenceStore extends PreferenceStore {
	private Session session;

	/** Retrieves the preference node */
	protected Node getPreferenceNode() {
		try {
			Node userHome = JcrUtils.getUserHome(session);
			Node preferences;
			if (!userHome.hasNode(ArgeoNames.ARGEO_PREFERENCES)) {
				preferences = userHome.addNode(ArgeoNames.ARGEO_PREFERENCES);
				session.save();
			} else {
				preferences = userHome.getNode(ArgeoNames.ARGEO_PREFERENCES);
			}
			return preferences;
		} catch (RepositoryException e) {
			JcrUtils.discardQuietly(session);
			throw new ArgeoException("Cannot retrieve preferences", e);
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
				if (!p.isMultiple()) {
					props.setProperty(p.getName(), p.getValue().getString());
				}
			}
			out = new ByteArrayOutputStream();
			props.store(out, "");
			in = new ByteArrayInputStream(out.toByteArray());
			load(in);
		} catch (Exception e) {
			throw new ArgeoException("Cannot load preferences", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	public void save() throws IOException {
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		Node preferences = null;
		try {
			out = new ByteArrayOutputStream();
			save(out, "");
			in = new ByteArrayInputStream(out.toByteArray());
			Properties props = new Properties();
			props.load(in);
			preferences = getPreferenceNode();
			for (Object key : props.keySet()) {
				String name = key.toString();
				String value = props.getProperty(name);
				preferences.setProperty(name, value);
			}
			preferences.getSession().save();
		} catch (Exception e) {
			JcrUtils.discardUnderlyingSessionQuietly(preferences);
			throw new ArgeoException("Cannot load preferences", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	public void setSession(Session session) {
		this.session = session;
	}

}
