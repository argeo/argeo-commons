package org.argeo.internal.cms.dbus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.argeo.api.cms.dbus.CmsDBusConnection;
import org.argeo.api.cms.freedesktop.FreeDesktopApplication;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

public class CmsDBusConnectionImpl implements CmsDBusConnection {
	private final DBusConnection dBusConnection;

	public CmsDBusConnectionImpl(DBusConnection dBusConnection) {
		this.dBusConnection = dBusConnection;
	}

	@Override
	public void close() throws IOException {
		dBusConnection.close();
	}

	@Override
	public void requestBusName(String _busname) {
		try {
			dBusConnection.requestBusName(_busname);
		} catch (DBusException e) {
			throw new IllegalArgumentException("Cannot request DBus " + _busname, e);
		}
	}

	@Override
	public void exportObject(String _objectPath, Object _object) {
		try {
			if (_object instanceof DBusInterface dBusInterface) {
				dBusConnection.exportObject(dBusInterface);
			} else if (_object instanceof FreeDesktopApplication freeDesktopApplication) {
				dBusConnection.exportObject(new FreeDesktopApplicationInterface() {

					@Override
					public String getObjectPath() {
						return freeDesktopApplication.getObjectPath();
					}

					@Override
					public void open(List<String> uris, Map<String, Variant<?>> platformData) {
						freeDesktopApplication.open(uris);
					}

					@Override
					public void activateAction(String actionName, List<Variant<?>> parameter,
							Map<String, Variant<?>> platformData) {
						freeDesktopApplication.activateAction(actionName);
					}

					@Override
					public void activate(Map<String, Variant<?>> platformData) {
						freeDesktopApplication.activate();
					}
				});
			} else {
				throw new IllegalArgumentException("Unrecognized DBus interface " + _object.getClass());
			}
		} catch (DBusException e) {
			throw new IllegalArgumentException("Cannot export to DBus object " + _objectPath, e);
		}

	}

//	@Override
	public void callMethodAsync(Object _object, String _method, Object... _parameters) {
		dBusConnection.callMethodAsync((DBusInterface) _object, _method, _parameters);

	}

	/*
	 * STATIC METHODS
	 */
//	static CmsDBusConnectionImpl sessionBus() {
//		try {
//			/* Get a connection to the session bus so we can request a bus name */
//			DBusConnection dBusConnection = DBusConnectionBuilder.forSessionBus().build();
////		m_conn = DBusConnectionBuilder.forAddress("unix:path=/tmp/dbus-80908265778467677465").build();
////		m_conn = DBusConnectionBuilder.forAddress("tcp:host=localhost,port=55556").build();
//			return new CmsDBusConnectionImpl(dBusConnection);
//		} catch (DBusException e) {
//			throw new IllegalStateException("Cannot get session bus", e);
//		}
//
//	}

}
