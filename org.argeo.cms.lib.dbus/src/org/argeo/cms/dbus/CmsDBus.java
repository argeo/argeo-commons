package org.argeo.cms.dbus;

import org.freedesktop.dbus.connections.impl.DBusConnection;

public interface CmsDBus {
	final static String DBUS_SESSION_BUS_ADDRESS = "DBUS_SESSION_BUS_ADDRESS";
	
	DBusConnection openSessionConnection();
}
