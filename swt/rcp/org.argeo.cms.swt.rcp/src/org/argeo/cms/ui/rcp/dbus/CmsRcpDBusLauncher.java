package org.argeo.cms.ui.rcp.dbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.argeo.api.cms.CmsApp;
import org.argeo.cms.dbus.CmsDBus;

public class CmsRcpDBusLauncher {
	private CompletableFuture<CmsDBus> cmsDBus = new CompletableFuture<>();

	private Map<String, CmsRcpFreeDesktopApplication> apps = new HashMap<>();

	public void start() {

	}

	public void stop() {

	}

	public void addCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		final String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		cmsDBus.thenAcceptAsync((cmsDBus) -> {
			CmsRcpFreeDesktopApplication application = new CmsRcpFreeDesktopApplication(cmsDBus, contextName, cmsApp);
			apps.put(contextName, application);
		});
	}

	public void removeCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		final String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		CmsRcpFreeDesktopApplication application = apps.remove(contextName);
		if (application != null) {
			try {
				application.close();
			} catch (IOException e) {
				throw new IllegalStateException("Cannot remove CMS RCP app " + contextName, e);
			}
		}
	}

	public void setCmsDBus(CmsDBus cmsDBus) {
		this.cmsDBus.complete(cmsDBus);
	}

}
