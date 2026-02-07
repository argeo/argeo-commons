package org.argeo.internal.cms.dbus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.cms.dbus.CmsDBus;
import org.argeo.api.cms.dbus.CmsDBusConnection;
import org.argeo.cms.util.OS;
import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

public class CmsDBusImpl implements CmsDBus {
	private final static CmsLog log = CmsLog.getLog(CmsDBusImpl.class);
	final static Path EMBEDDED_SESSION_BUS_ADDRESS = OS.getRunDir().resolve("bus");
	final static String DBUS_SESSION_BUS_ADDRESS = "DBUS_SESSION_BUS_ADDRESS";

	private BusAddress sessionBusAddress;

	private EmbeddedDBusDaemon dBusDaemon;
	private Path dBusDaemonSocket;

	public void start() {
		try {
			final String envSessionBusAddress = System.getenv(DBUS_SESSION_BUS_ADDRESS);
			if (envSessionBusAddress != null) {
				sessionBusAddress = BusAddress.of(envSessionBusAddress);

				// !! We must first initialise a connection, otherwise there are classloader
				// issues later on
				try (DBusConnection dBusConnection = DBusConnectionBuilder.forAddress(sessionBusAddress)
						.withShared(false).build()) {

				}
				log.debug(() -> "Found session DBus with address " + sessionBusAddress);
			} else {
				Path socketLocation = EMBEDDED_SESSION_BUS_ADDRESS;
				if (Files.exists(socketLocation))
					Files.delete(socketLocation);
				else
					Files.createDirectories(socketLocation.getParent());

				String embeddedSessionBusAddress = "unix:path=" + socketLocation.toString();
				dBusDaemon = new EmbeddedDBusDaemon(embeddedSessionBusAddress + ",listen=true");
				dBusDaemon.startInBackgroundAndWait(30 * 1000);
				dBusDaemonSocket = socketLocation;

				sessionBusAddress = BusAddress.of(embeddedSessionBusAddress);
				try (DBusConnection dBusConnection = DBusConnectionBuilder.forAddress(sessionBusAddress)
						.withShared(false).build()) {

				}
				log.debug(() -> "Started embedded session DBus with address " + sessionBusAddress);

				// TODO set environment variable?
			}
		} catch (DBusException | IOException e) {
			throw new IllegalStateException("Cannot find a session bus", e);
		}
	}

	public void stop() {
		if (dBusDaemon != null) {
			try {
				dBusDaemon.close();
			} catch (IOException e) {
				log.error("Cannot close embedded DBus daemon", e);
			}
			try {
				Files.delete(dBusDaemonSocket);
			} catch (IOException e) {
				log.error("Cannot delete DBus daemon socket " + dBusDaemonSocket, e);
			}
		}
	}

	@Override
	public CmsDBusConnection openSessionConnection() {
		try {
			DBusConnection dBusConnection = DBusConnectionBuilder.forAddress(sessionBusAddress).withShared(false)
					.build();
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
		String address = System.getenv(CmsDBusImpl.DBUS_SESSION_BUS_ADDRESS);
		if (address == null)
			address = "unix:path=" + CmsDBusImpl.EMBEDDED_SESSION_BUS_ADDRESS.toString();
		return BusAddress.of(address);
	}

}
