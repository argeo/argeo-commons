package org.argeo.api.uuid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;

/**
 * An {@link UUID} factory whose node id (for time based UUIDs) is the hardware
 * MAC address as specified in RFC4122.
 * 
 * @see "https://datatracker.ietf.org/doc/html/rfc4122.html#section-4.1.6"
 */
public class MacAddressUuidFactory extends ConcurrentUuidFactory {
	public MacAddressUuidFactory() {
		this(0, localHardwareAddressAsNodeId());
	}

	public MacAddressUuidFactory(long initialClockRange) {
		this(initialClockRange, localHardwareAddressAsNodeId());
	}

	public MacAddressUuidFactory(byte[] hardwareAddress) {
		this(0, hardwareAddress);
	}

	public MacAddressUuidFactory(long initialClockRange, byte[] hardwareAddress) {
		super(initialClockRange, hardwareAddress);
	}

	private static byte[] localHardwareAddressAsNodeId() {
		InetAddress localHost;
		try {
			localHost = InetAddress.getLocalHost();
			NetworkInterface nic = NetworkInterface.getByInetAddress(localHost);
			if (nic != null && nic.getHardwareAddress() != null)
				return hardwareAddressToNodeId(nic);
			Enumeration<NetworkInterface> netInterfaces = null;
			netInterfaces = NetworkInterface.getNetworkInterfaces();
			if (netInterfaces == null || !netInterfaces.hasMoreElements())
				throw new IllegalStateException("No interfaces");
			while (netInterfaces.hasMoreElements()) {
				// TODO find out public/physical interfaces
				nic = netInterfaces.nextElement();
				if (nic.getHardwareAddress() != null)
					return hardwareAddressToNodeId(nic);
			}
			throw new IllegalStateException("No interfaces with a MAC address");
		} catch (UnknownHostException | SocketException e) {
			throw new IllegalStateException(e);
		}

	}

	public static byte[] hardwareAddressToNodeId(NetworkInterface nic) throws IllegalStateException {
		try {
			byte[] hardwareAddress = nic.getHardwareAddress();
			final int length = 6;
			byte[] arr = new byte[length];
			for (int i = 0; i < length; i++) {
				arr[i] = hardwareAddress[length - 1 - i];
			}
			return arr;
		} catch (SocketException e) {
			throw new IllegalStateException("Cannot retrieve hardware address from NIC", e);
		}
	}

}
