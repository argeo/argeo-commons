package org.eclipse.rap.rwt.client.service;

public interface BrowserNavigation extends ClientService {
	void pushState(String state, String title);

	void addBrowserNavigationListener(BrowserNavigationListener listener);
}
