package org.argeo.cms.dbus;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;

/** Access to a DBus session or system bus. */
public interface CmsDBus {

	DBusConnection openSessionConnection();

	public static BusAddress getSessionBusAddress() {
		String address = System.getenv(CmsDBusImpl.DBUS_SESSION_BUS_ADDRESS);
		if (address == null)
			address = "unix:path=" + CmsDBusImpl.EMBEDDED_SESSION_BUS_ADDRESS.toString();
		return BusAddress.of(address);
	}
}
