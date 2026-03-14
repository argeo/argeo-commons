package org.argeo.cms.ui.rcp.dbus;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.dbus.CmsDBus;
import org.argeo.api.cms.dbus.CmsDBusConnection;
import org.argeo.api.cms.freedesktop.FreeDesktopApplication;
import org.argeo.cms.ui.rcp.CmsRcpDisplayFactory;

/** Wrap a CMS app as a XDG FreeDesktop application, via DBus. */
public class CmsRcpFreeDesktopApplication implements FreeDesktopApplication, Closeable {
	private final static CmsLog log = CmsLog.getLog(CmsRcpFreeDesktopApplication.class);

	private final static String BASE_PATH = "/org/argeo/cms/";

	private String path;

	private CmsApp cmsApp;

	private CmsDBusConnection dBusConnection;

	private CmsRcpDisplayFactory cmsRcpDisplayFactory;

	public CmsRcpFreeDesktopApplication(CmsRcpDisplayFactory cmsRcpDisplayFactory, CmsDBus cmsDBus, String contextName,
			CmsApp cmsApp) {
		this.cmsRcpDisplayFactory = cmsRcpDisplayFactory;
		// TODO find a better prefix and/or make it customisable
		this.path = BASE_PATH + contextName;
		this.cmsApp = cmsApp;
		try {
			String appName = path.replace('/', '.').substring(1);
			dBusConnection = cmsDBus.openSessionConnection();
			dBusConnection.requestBusName(appName);
			dBusConnection.exportObject(getObjectPath(), this);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Cannot add CMS app " + path, e);
		}
	}

	@Override
	public String getObjectPath() {
		return path;
	}

	@Override
	public void close() throws IOException {
		if (dBusConnection != null)
			dBusConnection.close();
	}

	@Override
	public void activate() {
		// String uiName = path != null ? path.substring(path.lastIndexOf('/') + 1) :
		// "";
		String uiName = "app";
		cmsRcpDisplayFactory.openCmsApp(cmsApp, uiName, null);
	}

	@Override
	public void open(List<String> uris) {
		log.debug(() -> "Open " + uris);
	}

	@Override
	public void activateAction(String actionName) {
		log.debug(() -> "Activate action '" + actionName + "'");
	}

}
