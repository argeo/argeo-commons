package org.argeo.api.uuid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * An {@link UUID} factory whose node id (for time based UUIDs) is the hardware
 * MAC address as specified in RFC4122.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122.html#section-4.1.6
 */
public class MacAddressUuidFactory extends ConcurrentUuidFactory {
	public final static UuidFactory DEFAULT = new MacAddressUuidFactory();

	public MacAddressUuidFactory() {
		super(localHardwareAddressAsNodeId());
	}

	public static byte[] localHardwareAddressAsNodeId() {
		InetAddress localHost;
		try {
			localHost = InetAddress.getLocalHost();
			NetworkInterface nic = NetworkInterface.getByInetAddress(localHost);
			return hardwareAddressToNodeId(nic);
		} catch (UnknownHostException | SocketException e) {
			throw new IllegalStateException(e);
		}

	}

	public static byte[] hardwareAddressToNodeId(NetworkInterface nic) throws SocketException {
		byte[] hardwareAddress = nic.getHardwareAddress();
		final int length = 6;
		byte[] arr = new byte[length];
		for (int i = 0; i < length; i++) {
			arr[i] = hardwareAddress[length - 1 - i];
		}
		return arr;
	}

}
