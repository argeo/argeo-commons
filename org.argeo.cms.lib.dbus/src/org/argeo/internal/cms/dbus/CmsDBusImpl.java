package org.argeo.internal.cms.dbus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.dbus.CmsDBus;
import org.argeo.api.cms.dbus.CmsDBusConnection;
import org.argeo.cms.util.OS;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public class CmsDBusImpl implements CmsDBus {
	private final static CmsLog log = CmsLog.getLog(CmsDBusImpl.class);
	final static Path EMBEDDED_SESSION_BUS_ADDRESS = OS.getRunDir().resolve("embeddedbus");
	final static String DBUS_SESSION_BUS_ADDRESS = "DBUS_SESSION_BUS_ADDRESS";

	private BusAddress sessionBusAddress;

	private Path dBusDaemonSocket;

	public void start() {
		try {
			final String envSessionBusAddress = System.getenv(DBUS_SESSION_BUS_ADDRESS);
//			final String envSessionBusAddress = null;
			if (envSessionBusAddress != null) {
				sessionBusAddress = new BusAddress(envSessionBusAddress);

				DBusConnection.getConnection(DBusConnection.SESSION);
				// !! We must first initialise a connection, otherwise there are classloader
				// issues later on
//				try (DBusConnection dBusConnection = DBusConnectionBuilder.forAddress(sessionBusAddress)
//						.withShared(false).build()) {
//
//				}
				log.debug(() -> "Found session DBus with address " + sessionBusAddress);
			} else {
				Path socketLocation = EMBEDDED_SESSION_BUS_ADDRESS;

				if (Files.exists(socketLocation))
					Files.delete(socketLocation);
				else
					Files.createDirectories(socketLocation.getParent());

				String address = getSessionBusAddrStr();

				Thread daemonThread = new Thread(() -> {
					try {
						// The v2.7 test daemon blocks the thread
						org.freedesktop.dbus.bin.DBusDaemon
								.main(new String[] { "--print-address", "--unix", "--listen", address });
					} catch (Exception e) {
						throw new RuntimeException("Cannot create DBusDaemon", e);
					}
				});
				daemonThread.setDaemon(true);
				daemonThread.setName("DBus-Java-2.7-Daemon");
				daemonThread.start();

//				while (!Files.exists(socketLocation)) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// silent
				}
//				}

				dBusDaemonSocket = socketLocation;

				// DBusConnection dBusConnectionEmbedded =
				// DBusConnection.getConnection(address);
				log.debug(() -> "Started embedded session DBus with address " + sessionBusAddress);

//				// TODO set environment variable?
			}
		} catch (DBusException | ParseException | IOException e) {
			throw new IllegalStateException("Cannot find a session bus", e);
		}
	}

	public void stop() {
//		if (dBusDaemon != null) {
//			try {
//				dBusDaemon.close();
//			} catch (IOException e) {
//				log.error("Cannot close embedded DBus daemon", e);
//			}
//			try {
//				Files.delete(dBusDaemonSocket);
//			} catch (IOException e) {
//				log.error("Cannot delete DBus daemon socket " + dBusDaemonSocket, e);
//			}
//		}
	}

	@Override
	public CmsDBusConnection openSessionConnection() {
		try {
			DBusConnection dBusConnection = DBusConnection.getConnection(getSessionBusAddrStr());
			// TODO track all connections?
			return new CmsDBusConnectionImpl(dBusConnection);
		} catch (DBusException e) {
			e.printStackTrace();
			throw new IllegalStateException("Cannot open connection to session DBus", e);
		}
	}

	/*
	 * STATIC METHODS
	 */
	public static BusAddress getSessionBusAddress() {
		String address = getSessionBusAddrStr();
		try {
			return new BusAddress(address);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Cannot create DBus address " + address, e);
		}
	}

	public static String getSessionBusAddrStr() {
		String address = System.getenv(CmsDBusImpl.DBUS_SESSION_BUS_ADDRESS);
		//String address = null;
		if (address == null)
			address = "unix:path=" + CmsDBusImpl.EMBEDDED_SESSION_BUS_ADDRESS.toString() + ",guid=0";
		return address;
	}
}
