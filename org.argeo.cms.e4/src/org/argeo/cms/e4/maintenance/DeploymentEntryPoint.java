package org.argeo.cms.e4.maintenance;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.argeo.api.NodeConstants;
import org.argeo.api.NodeDeployment;
import org.argeo.api.NodeState;
import org.argeo.cms.ui.util.CmsUiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

class DeploymentEntryPoint {
	private final BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();

	protected void createContents(Composite parent) {
		// FIXME manage authentication if needed
		// if (!CurrentUser.roles().contains(AuthConstants.ROLE_ADMIN))
		// return;

		// parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isDesktop()) {
			parent.setLayout(new GridLayout(2, true));
		} else {
			// TODO add scrolling
			parent.setLayout(new GridLayout(1, true));
		}

		initHighLevelSummary(parent);

		Group securityGroup = createHighLevelGroup(parent, "Security");
		securityGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		new SecurityDeploymentUi(securityGroup, SWT.NONE);

		Group dataGroup = createHighLevelGroup(parent, "Data");
		dataGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		new DataDeploymentUi(dataGroup, SWT.NONE);

		Group logGroup = createHighLevelGroup(parent, "Notifications");
		logGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		new LogDeploymentUi(logGroup, SWT.NONE);

		Group connectivityGroup = createHighLevelGroup(parent, "Connectivity");
		new ConnectivityDeploymentUi(connectivityGroup, SWT.NONE);
		connectivityGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

	}

	private void initHighLevelSummary(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
		if (isDesktop())
			gridData.horizontalSpan = 3;
		composite.setLayoutData(gridData);
		composite.setLayout(new FillLayout());

		ServiceReference<NodeState> nodeStateRef = bc.getServiceReference(NodeState.class);
		if (nodeStateRef == null)
			throw new IllegalStateException("No CMS state available");
		NodeState nodeState = bc.getService(nodeStateRef);
		ServiceReference<NodeDeployment> nodeDeploymentRef = bc.getServiceReference(NodeDeployment.class);
		Label label = new Label(composite, SWT.WRAP);
		CmsUiUtils.markup(label);
		if (nodeDeploymentRef == null) {
			label.setText("Not yet deployed on <br>" + nodeState.getHostname() + "</br>, please configure below.");
		} else {
			Object stateUuid = nodeStateRef.getProperty(NodeConstants.CN);
			NodeDeployment nodeDeployment = bc.getService(nodeDeploymentRef);
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTimeInMillis(nodeDeployment.getAvailableSince());
			calendar.setTimeZone(TimeZone.getDefault());
			label.setText("[" + "<b>" + nodeState.getHostname() + "</b>]# " + "Deployment state " + stateUuid
					+ ", available since <b>" + calendar.getTime() + "</b>");
		}
	}

	private static Group createHighLevelGroup(Composite parent, String text) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(text);
		CmsUiUtils.markup(group);
		return group;
	}

	private boolean isDesktop() {
		return true;
	}
}
