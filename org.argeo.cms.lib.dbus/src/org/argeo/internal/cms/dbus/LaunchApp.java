package org.argeo.internal.cms.dbus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.argeo.api.cms.freedesktop.FreeDesktopApplication;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

/** Launch a FreeDesktop application. */
public class LaunchApp {
	private final static Method activateMethod;
	static {
		try {
			// make sure the method is properly referenced
			activateMethod = FreeDesktopApplicationInterface.class.getMethod("activate", Map.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("Method not found for " + FreeDesktopApplication.class);
		}
	}

	private final DBusConnection dBusConnection;
	private final String path;
	private final String busName;

	private LaunchApp(DBusConnection dBusConnection, String path) {
		this.dBusConnection = dBusConnection;
		this.path = path;
		// TODO understand the difference between bus name and path
		this.busName = path.replace('/', '.').substring(1);
	}

	private void launch() {
		// we retrieve the app each time as it could be dynamic
		FreeDesktopApplicationInterface app;
		try {
			app = dBusConnection.getRemoteObject(busName, path, FreeDesktopApplicationInterface.class);
		} catch (DBusException e) {
			throw new IllegalArgumentException("App " + path + " cannot be found", e);
		}
		dBusConnection.callMethodAsync(app, activateMethod.getName(), new HashMap<String, Variant<?>>());
	}

	public static void main(String[] args) throws Exception {
		// TODO improve help / usage
		if (args.length == 0)
			throw new IllegalArgumentException("An application path or a context name must be specified.");
		String path = args[0];
		if (!path.startsWith("/"))
			path = "/org/argeo/cms/" + path;
		BusAddress busAddress = CmsDBusImpl.getSessionBusAddress();
		try (DBusConnection dBusConnection = DBusConnectionBuilder.forAddress(busAddress).withShared(false).build()) {
			LaunchApp launchApp = new LaunchApp(dBusConnection, path);
			launchApp.launch();
		}
	}

}
