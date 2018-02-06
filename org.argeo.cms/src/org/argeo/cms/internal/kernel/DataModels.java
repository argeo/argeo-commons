package org.argeo.cms.internal.kernel;

import static org.argeo.node.DataModelNamespace.CMS_DATA_MODEL_NAMESPACE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.argeo.cms.CmsException;
import org.argeo.node.DataModelNamespace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

class DataModels implements BundleListener {
	private Map<String, DataModel> dataModels = new TreeMap<>();

	public DataModels(BundleContext bc) {
		for (Bundle bundle : bc.getBundles())
			processBundle(bundle);
		bc.addBundleListener(this);
	}

	public List<DataModel> getNonAbstractDataModels() {
		List<DataModel> res = new ArrayList<>();
		for (String name : dataModels.keySet()) {
			DataModel dataModel = dataModels.get(name);
			if (!dataModel.isAbstract())
				res.add(dataModel);
		}
		// TODO reorder?
		return res;
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == Bundle.RESOLVED) {
			processBundle(event.getBundle());
		} else if (event.getType() == Bundle.UNINSTALLED) {
			BundleWiring wiring = event.getBundle().adapt(BundleWiring.class);
			List<BundleCapability> providedDataModels = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
			if (providedDataModels.size() == 0)
				return;
			for (BundleCapability bundleCapability : providedDataModels) {
				dataModels.remove(bundleCapability.getAttributes().get(DataModelNamespace.NAME));
			}
		}

	}

	protected void processBundle(Bundle bundle) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		List<BundleCapability> providedDataModels = wiring.getCapabilities(CMS_DATA_MODEL_NAMESPACE);
		if (providedDataModels.size() == 0)
			return;
		List<BundleWire> requiredDataModels = wiring.getRequiredWires(CMS_DATA_MODEL_NAMESPACE);
		for (BundleCapability bundleCapability : providedDataModels) {
			DataModel dataModel = new DataModel(bundleCapability, requiredDataModels);
			dataModels.put(dataModel.getName(), dataModel);
		}
	}

	/** Return a negative depth if dataModel is required by ref, 0 otherwise. */
	static int required(DataModel ref, DataModel dataModel, int depth) {
		for (DataModel dm : ref.getRequired()) {
			if (dm.equals(dataModel))// found here
				return depth - 1;
			int d = required(dm, dataModel, depth - 1);
			if (d != 0)// found deeper
				return d;
		}
		return 0;// not found
	}

	class DataModel {
		private final String name;
		private final boolean abstrct;
		// private final boolean standalone;
		private final String cnd;
		private final List<DataModel> required;

		private DataModel(BundleCapability bundleCapability, List<BundleWire> requiredDataModels) {
			assert CMS_DATA_MODEL_NAMESPACE.equals(bundleCapability.getNamespace());
			Map<String, Object> attrs = bundleCapability.getAttributes();
			name = (String) attrs.get(DataModelNamespace.NAME);
			assert name != null;
			abstrct = KernelUtils.asBoolean((String) attrs.get(DataModelNamespace.ABSTRACT));
			// standalone = KernelUtils.asBoolean((String)
			// attrs.get(DataModelNamespace.CAPABILITY_STANDALONE_ATTRIBUTE));
			cnd = (String) attrs.get(DataModelNamespace.CND);
			List<DataModel> req = new ArrayList<>();
			for (BundleWire wire : requiredDataModels) {
				String requiredDataModelName = (String) wire.getCapability().getAttributes()
						.get(DataModelNamespace.NAME);
				assert requiredDataModelName != null;
				DataModel requiredDataModel = dataModels.get(requiredDataModelName);
				if (requiredDataModel == null)
					throw new CmsException("No required data model " + requiredDataModelName);
				req.add(requiredDataModel);
			}
			required = Collections.unmodifiableList(req);
		}

		public String getName() {
			return name;
		}

		public boolean isAbstract() {
			return abstrct;
		}

		// public boolean isStandalone() {
		// return !isAbstract();
		// }

		public String getCnd() {
			return cnd;
		}

		public List<DataModel> getRequired() {
			return required;
		}

		// @Override
		// public int compareTo(DataModel o) {
		// if (equals(o))
		// return 0;
		// int res = required(this, o, 0);
		// if (res != 0)
		// return res;
		// // the other way round
		// res = required(o, this, 0);
		// if (res != 0)
		// return -res;
		// return 0;
		// }

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DataModel)
				return ((DataModel) obj).name.equals(name);
			return false;
		}

		@Override
		public String toString() {
			return "Data model " + name;
		}

	}

}
