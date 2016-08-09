package org.argeo.cms.maintenance;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.cms.CmsException;
import org.argeo.cms.internal.kernel.JackrabbitType;
import org.argeo.cms.util.CmsUtils;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.node.NodeConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

class DataDeploymentUi extends AbstractOsgiComposite {
	private static final long serialVersionUID = 590221539553514693L;

	public DataDeploymentUi(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initUi(int style) {
		if (isDeployed()) {
			initCurrentUi(this);
		} else {
			initNewUi(this);
		}
	}

	private void initNewUi(Composite parent) {
		try {
			ConfigurationAdmin confAdmin = bc.getService(bc.getServiceReference(ConfigurationAdmin.class));
			Configuration[] confs = confAdmin.listConfigurations(
					"(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + NodeConstants.JACKRABBIT_FACTORY_PID + ")");
			if (confs == null || confs.length == 0) {
				Group buttonGroup = new Group(parent, SWT.NONE);
				buttonGroup.setText("Repository Type");
				buttonGroup.setLayout(new GridLayout(2, true));
				buttonGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));

				SelectionListener selectionListener = new SelectionAdapter() {
					private static final long serialVersionUID = 6247064348421088092L;

					public void widgetSelected(SelectionEvent event) {
						Button radio = (Button) event.widget;
						if (!radio.getSelection())
							return;
						log.debug(event);
						JackrabbitType nodeType = (JackrabbitType) radio.getData();
						if (log.isDebugEnabled())
							log.debug(" selected = " + nodeType.name());
					};
				};

				for (JackrabbitType nodeType : JackrabbitType.values()) {
					Button radio = new Button(buttonGroup, SWT.RADIO);
					radio.setText(nodeType.name());
					radio.setData(nodeType);
					if (nodeType.equals(JackrabbitType.localfs))
						radio.setSelection(true);
					radio.addSelectionListener(selectionListener);
				}

			} else if (confs.length == 1) {

			} else {
				throw new CmsException("Multiple repos not yet supported");
			}
		} catch (Exception e) {
			throw new CmsException("Cannot initialize UI", e);
		}

	}

	private void initCurrentUi(Composite parent) {
		parent.setLayout(new GridLayout());
		Collection<ServiceReference<RepositoryContext>> contexts = getServiceReferences(RepositoryContext.class,
				"(" + ArgeoJcrConstants.JCR_REPOSITORY_ALIAS + "=*)");
		StringBuffer text = new StringBuffer();
		text.append("<span style='font-variant: small-caps;'>Jackrabbit Repositories</span><br/>");
		for (ServiceReference<RepositoryContext> sr : contexts) {
			RepositoryContext repositoryContext = bc.getService(sr);
			String alias = sr.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS).toString();
			String rootNodeId = repositoryContext.getRootNodeId().toString();
			RepositoryConfig repositoryConfig = repositoryContext.getRepositoryConfig();
			Path repoHomePath = new File(repositoryConfig.getHomeDir()).toPath().toAbsolutePath();
			// TODO check data store

			text.append("<b>" + alias + "</b><br/>");
			text.append("rootNodeId: " + rootNodeId + "<br/>");
			try {
				FileStore fileStore = Files.getFileStore(repoHomePath);
				text.append("partition: " + fileStore.toString() + "<br/>");
				text.append(
						percentUsed(fileStore) + " used (" + humanReadable(fileStore.getUsableSpace()) + " free)<br/>");
			} catch (IOException e) {
				log.error("Cannot check fileStore for " + repoHomePath, e);
			}
		}
		Label label = new Label(parent, SWT.NONE);
		label.setData(new GridData(SWT.FILL, SWT.FILL, false, false));
		CmsUtils.markup(label);
		label.setText("<span style=''>" + text.toString() + "</span>");
	}

	private String humanReadable(long bytes) {
		long mb = bytes / (1024 * 1024);
		return mb >= 2048 ? Long.toString(mb / 1024) + " GB" : Long.toString(mb) + " MB";
	}

	private String percentUsed(FileStore fs) throws IOException {
		long used = fs.getTotalSpace() - fs.getUnallocatedSpace();
		long percent = used * 100 / fs.getTotalSpace();
		if (log.isTraceEnabled()) {
			// output identical to `df -B 1`)
			log.trace(fs.getTotalSpace() + "," + used + "," + fs.getUsableSpace());
		}
		String span;
		if (percent < 80)
			span = "<span style='color:green;font-weight:bold'>";
		else if (percent < 95)
			span = "<span style='color:orange;font-weight:bold'>";
		else
			span = "<span style='color:red;font-weight:bold'>";
		return span + percent + "%</span>";
	}

	protected boolean isDeployed() {
		return bc.getServiceReference(RepositoryContext.class) != null;
	}

}
