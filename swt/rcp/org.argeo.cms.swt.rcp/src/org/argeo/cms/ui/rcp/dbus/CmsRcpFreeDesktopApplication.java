package org.argeo.cms.ui.rcp.dbus;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.argeo.api.cms.CmsApp;
import org.argeo.cms.dbus.CmsDBus;
import org.argeo.cms.freedesktop.FreeDesktopApplication;
import org.argeo.cms.ui.rcp.CmsRcpDisplayFactory;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

public class CmsRcpFreeDesktopApplication implements FreeDesktopApplication, Closeable {
	private String path;

	private CmsApp cmsApp;

	private DBusConnection dBusConnection;

	public CmsRcpFreeDesktopApplication(CmsDBus cmsDBus, String contextName, CmsApp cmsApp) {
		// TODO find a better prefix and/or make it customisable
		this.path = "/org/argeo/cms/" + contextName;
		this.cmsApp = cmsApp;
		try {
			String appName = path.replace('/', '.').substring(1);
			dBusConnection = cmsDBus.openSessionConnection();
			dBusConnection.requestBusName(appName);
			dBusConnection.exportObject(getObjectPath(), this);
		} catch (DBusException e) {
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
	public void activate(Map<String, Variant<?>> platformData) {
		// String uiName = path != null ? path.substring(path.lastIndexOf('/') + 1) :
		// "";
		String uiName = "app";
		CmsRcpDisplayFactory.openCmsApp(cmsApp, uiName, null);
	}

	@Override
	public void open(List<String> uris, Map<String, Variant<?>> platformData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activateAction(String actionName, List<Variant<?>> parameter, Map<String, Variant<?>> platformData) {
		// TODO Auto-generated method stub

	}

}
