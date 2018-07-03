package org.argeo.eclipse.ui.rcp.internal.rwt;

import org.eclipse.rap.rwt.client.Client;
import org.eclipse.rap.rwt.client.service.BrowserNavigation;
import org.eclipse.rap.rwt.client.service.BrowserNavigationListener;
import org.eclipse.rap.rwt.client.service.ClientService;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;

public class RcpClient implements Client {

	@Override
	public <T extends ClientService> T getService(Class<T> type) {
		if (type.isAssignableFrom(JavaScriptExecutor.class))
			return (T) javaScriptExecutor;
		else if (type.isAssignableFrom(BrowserNavigation.class))
			return (T) browserNavigation;
		else
			return null;
	}

	private JavaScriptExecutor javaScriptExecutor = new JavaScriptExecutor() {

		@Override
		public void execute(String code) {
			// TODO Auto-generated method stub

		}
	};
	private BrowserNavigation browserNavigation = new BrowserNavigation() {

		@Override
		public void pushState(String state, String title) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addBrowserNavigationListener(
				BrowserNavigationListener listener) {
			// TODO Auto-generated method stub

		}
	};
}
