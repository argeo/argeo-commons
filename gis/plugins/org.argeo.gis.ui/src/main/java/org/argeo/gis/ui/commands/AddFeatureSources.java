package org.argeo.gis.ui.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.AbstractTreeContentProvider;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.geotools.jcr.GeoJcrMapper;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class AddFeatureSources extends AbstractHandler {
	private GeoJcrMapper geoJcrMapper;
	private Session session;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			FeatureSourceChooserDialog dialog = new FeatureSourceChooserDialog(
					HandlerUtil.getActiveShell(event));
			if (dialog.open() == Dialog.OK) {
				Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> featureSources = dialog
						.getFeatureSources();
				for (String alias : featureSources.keySet()) {
					for (FeatureSource<SimpleFeatureType, SimpleFeature> fs : featureSources
							.get(alias)) {
						Node fsNode = geoJcrMapper.getFeatureSourceNode(
								session, alias, fs);
						try {
							fsNode.getSession().save();
						} catch (RepositoryException e) {
							throw new ArgeoException("Cannot save " + fsNode, e);
						}
					}
				}
			}
		} catch (Exception e) {
			Error.show("Cannot add new feature source", e);
		}
		return null;
	}

	public void setGeoJcrMapper(GeoJcrMapper geoJcrMapper) {
		this.geoJcrMapper = geoJcrMapper;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	class FeatureSourceChooserDialog extends TitleAreaDialog {
		private TreeViewer viewer;
		private Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> featureSources = new HashMap<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>>();

		public FeatureSourceChooserDialog(Shell parentShell) {
			super(parentShell);
		}

		protected Point getInitialSize() {
			return new Point(300, 400);
		}

		protected Control createDialogArea(Composite parent) {
			setTitle("Feature Source");
			setMessage("Select or or many feature sources to register");
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			Composite composite = new Composite(dialogarea, SWT.NONE);
			composite.setLayout(new GridLayout(1, false));
			composite
					.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			viewer = new TreeViewer(composite);
			viewer.getTree().setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, true));
			viewer.setContentProvider(new DataStoreContentProvider());
			viewer.setLabelProvider(new DataStoreLabelProvider());
			viewer.setInput(geoJcrMapper.getPossibleFeatureSources());
			parent.pack();
			return composite;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void okPressed() {
			Iterator<Object> it = ((IStructuredSelection) viewer.getSelection())
					.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof FeatureSourceNode) {
					FeatureSourceNode fsn = (FeatureSourceNode) obj;
					String alias = fsn.getDataStoreAlias();
					if (!featureSources.containsKey(alias))
						featureSources
								.put(alias,
										new ArrayList<FeatureSource<SimpleFeatureType, SimpleFeature>>());
					featureSources.get(alias).add(fsn.getFeatureSource());
				} else {
					// data store node
					String alias = obj.toString();
					featureSources.put(alias, geoJcrMapper
							.getPossibleFeatureSources().get(alias));
				}
			}
			super.okPressed();
		}

		public Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> getFeatureSources() {
			return featureSources;
		}
	}

	private class DataStoreContentProvider extends AbstractTreeContentProvider {

		@SuppressWarnings("unchecked")
		public Object[] getElements(Object inputElement) {
			List<TreeParent> dataStoreNodes = new ArrayList<TreeParent>();
			Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>> featureSources = (Map<String, List<FeatureSource<SimpleFeatureType, SimpleFeature>>>) inputElement;
			for (String alias : featureSources.keySet()) {
				TreeParent dataStoreNode = new TreeParent(alias);
				for (FeatureSource<SimpleFeatureType, SimpleFeature> featureSource : featureSources
						.get(alias)) {
					dataStoreNode.addChild(new FeatureSourceNode(alias,
							featureSource));
				}
				dataStoreNodes.add(dataStoreNode);
			}
			return dataStoreNodes.toArray();
		}

	}

	private class DataStoreLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			return super.getText(element);
		}

	}

	private class FeatureSourceNode extends TreeParent {
		private final String dataStoreAlias;
		private final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

		public FeatureSourceNode(String dataStoreAlias,
				FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
			super(featureSource.getName().toString());
			this.dataStoreAlias = dataStoreAlias;
			this.featureSource = featureSource;
		}

		public String getDataStoreAlias() {
			return dataStoreAlias;
		}

		public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
			return featureSource;
		}

	}

}
