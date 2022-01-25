package org.argeo.cms.acr;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.BitSet;
import java.util.Enumeration;

import org.argeo.api.cms.CmsLog;
import org.argeo.api.uuid.ConcurrentUuidFactory;
import org.argeo.api.uuid.UuidBinaryUtils;

public class CmsUuidFactory extends ConcurrentUuidFactory {
	private final static CmsLog log = CmsLog.getLog(CmsUuidFactory.class);

	public CmsUuidFactory(byte[] nodeId) {
		super(nodeId);
		assert newTimeUUID().node() == BitSet.valueOf(toNodeIdBytes(nodeId, 0)).toLongArray()[0];
	}

	public CmsUuidFactory() {
		this(getIpBytes());
	}

	/** Returns an SHA1 digest of one of the IP addresses. */
	protected static byte[] getIpBytes() {
		Enumeration<NetworkInterface> netInterfaces = null;
		try {
			netInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new IllegalStateException(e);
		}
		if (netInterfaces == null)
			throw new IllegalStateException("No interfaces");

		InetAddress selectedIpv6 = null;
		InetAddress selectedIpv4 = null;
		netInterfaces: while (netInterfaces.hasMoreElements()) {
			NetworkInterface netInterface = netInterfaces.nextElement();
			byte[] hardwareAddress = null;
			try {
				hardwareAddress = netInterface.getHardwareAddress();
				if (hardwareAddress != null) {
					// first IPv6
					addr: for (InterfaceAddress addr : netInterface.getInterfaceAddresses()) {
						InetAddress ip = addr.getAddress();
						if (ip instanceof Inet6Address) {
							Inet6Address ipv6 = (Inet6Address) ip;
							if (ipv6.isAnyLocalAddress() || ipv6.isLinkLocalAddress() || ipv6.isLoopbackAddress())
								continue addr;
							selectedIpv6 = ipv6;
							break netInterfaces;
						}

					}
					// then IPv4
					addr: for (InterfaceAddress addr : netInterface.getInterfaceAddresses()) {
						InetAddress ip = addr.getAddress();
						if (ip instanceof Inet4Address) {
							Inet4Address ipv4 = (Inet4Address) ip;
							if (ipv4.isAnyLocalAddress() || ipv4.isLinkLocalAddress() || ipv4.isLoopbackAddress())
								continue addr;
							selectedIpv4 = ipv4;
						}

					}
				}
			} catch (SocketException e) {
				throw new IllegalStateException(e);
			}
		}
		InetAddress selectedIp = selectedIpv6 != null ? selectedIpv6 : selectedIpv4;
		if (selectedIp == null)
			throw new IllegalStateException("No IP address found");
		byte[] digest = sha1(selectedIp.getAddress());
		log.info("Use IP " + selectedIp + " hashed as " + UuidBinaryUtils.toHexString(digest) + " as node id");
		byte[] nodeId = toNodeIdBytes(digest, 0);
		// marks that this is not based on MAC address
		forceToNoMacAddress(nodeId, 0);
		return nodeId;
	}

}
