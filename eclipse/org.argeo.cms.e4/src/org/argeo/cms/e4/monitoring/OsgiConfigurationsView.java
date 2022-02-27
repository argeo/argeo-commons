package org.argeo.cms.e4.monitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;

import javax.annotation.PostConstruct;

import org.argeo.cms.CmsException;
import org.argeo.util.LangUtils;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class OsgiConfigurationsView {
	private final static BundleContext bc = FrameworkUtil.getBundle(OsgiConfigurationsView.class).getBundleContext();

	@PostConstruct
	public void createPartControl(Composite parent) {
		ConfigurationAdmin configurationAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));

		TreeViewer viewer = new TreeViewer(parent);
		// viewer.getTree().setHeaderVisible(true);

		TreeViewerColumn tvc = new TreeViewerColumn(viewer, SWT.NONE);
		tvc.getColumn().setWidth(400);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = 835407996597566763L;

			@Override
			public String getText(Object element) {
				if (element instanceof Configuration) {
					return ((Configuration) element).getPid();
				} else if (element instanceof Prop) {
					return ((Prop) element).key;
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof Configuration)
					return OsgiExplorerImages.CONFIGURATION;
				return null;
			}

		});

		tvc = new TreeViewerColumn(viewer, SWT.NONE);
		tvc.getColumn().setWidth(400);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			private static final long serialVersionUID = 6999659261190014687L;

			@Override
			public String getText(Object element) {
				if (element instanceof Configuration) {
					// return ((Configuration) element).getFactoryPid();
					return null;
				} else if (element instanceof Prop) {
					return ((Prop) element).value.toString();
				}
				return super.getText(element);
			}
		});

		viewer.setContentProvider(new ConfigurationsContentProvider());
		viewer.setInput(configurationAdmin);
	}

	static class ConfigurationsContentProvider implements ITreeContentProvider {
		private static final long serialVersionUID = -4892768279440981042L;
		private ConfigurationComparator configurationComparator = new ConfigurationComparator();

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) inputElement;
			try {
				Configuration[] configurations = configurationAdmin.listConfigurations(null);
				Arrays.sort(configurations, configurationComparator);
				return configurations;
			} catch (IOException | InvalidSyntaxException e) {
				throw new CmsException("Cannot list configurations", e);
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Configuration) {
				List<Prop> res = new ArrayList<>();
				Configuration configuration = (Configuration) parentElement;
				Dictionary<String, Object> props = configuration.getProperties();
				keys: for (String key : LangUtils.keys(props)) {
					if (Constants.SERVICE_PID.equals(key))
						continue keys;
					if (ConfigurationAdmin.SERVICE_FACTORYPID.equals(key))
						continue keys;
					res.add(new Prop(configuration, key, props.get(key)));
				}
				return res.toArray(new Prop[res.size()]);
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Prop)
				return ((Prop) element).configuration;
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof Configuration)
				return true;
			return false;
		}

	}

	static class Prop {
		final Configuration configuration;
		final String key;
		final Object value;

		public Prop(Configuration configuration, String key, Object value) {
			this.configuration = configuration;
			this.key = key;
			this.value = value;
		}

	}

	static class ConfigurationComparator implements Comparator<Configuration> {

		@Override
		public int compare(Configuration o1, Configuration o2) {
			return o1.getPid().compareTo(o2.getPid());
		}

	}
}
