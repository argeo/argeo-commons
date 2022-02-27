package org.argeo.cms.e4.maintenance;

import org.argeo.cms.swt.CmsSwtUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.useradmin.UserAdmin;

class ConnectivityDeploymentUi extends AbstractOsgiComposite {
	private static final long serialVersionUID = 590221539553514693L;

	public ConnectivityDeploymentUi(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	protected void initUi(int style) {
		StringBuffer text = new StringBuffer();
		text.append("<span style='font-variant: small-caps;'>Provided Servers</span><br/>");

		ServiceReference<HttpService> userAdminRef = bc.getServiceReference(HttpService.class);
		if (userAdminRef != null) {
			// FIXME use constants
			Object httpPort = userAdminRef.getProperty("http.port");
			Object httpsPort = userAdminRef.getProperty("https.port");
			if (httpPort != null)
				text.append("<b>http</b> ").append(httpPort).append("<br/>");
			if (httpsPort != null)
				text.append("<b>https</b> ").append(httpsPort).append("<br/>");

		}

		text.append("<br/>");
		text.append("<span style='font-variant: small-caps;'>Referenced Servers</span><br/>");

		Label label = new Label(this, SWT.NONE);
		label.setData(new GridData(SWT.FILL, SWT.FILL, false, false));
		CmsSwtUtils.markup(label);
		label.setText(text.toString());
	}

	protected boolean isDeployed() {
		return bc.getServiceReference(UserAdmin.class) != null;
	}
}
