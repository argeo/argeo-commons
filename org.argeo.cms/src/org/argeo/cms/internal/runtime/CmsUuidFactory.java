package org.argeo.cms.internal.runtime;

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
import org.argeo.api.uuid.NodeIdSupplier;
import org.argeo.api.uuid.UuidBinaryUtils;

@Deprecated
class CmsUuidFactory extends ConcurrentUuidFactory {
	private final static CmsLog log = CmsLog.getLog(CmsUuidFactory.class);

	public CmsUuidFactory(byte[] nodeId) {
		super(0, nodeId);
		assert createTimeUUID().node() == BitSet.valueOf(NodeIdSupplier.toNodeIdBytes(nodeId, 0)).toLongArray()[0];
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

		InetAddress selectedIpv6 = null;
		InetAddress selectedIpv4 = null;
		if (netInterfaces != null) {
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
								// we keep searching for IPv6
							}

						}
					}
				} catch (SocketException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		InetAddress selectedIp = selectedIpv6 != null ? selectedIpv6 : selectedIpv4;
		if (selectedIp == null) {
			log.warn("No IP address found, using a random node id for UUID generation");
			return NodeIdSupplier.randomNodeId();
		}
		byte[] digest = sha1(selectedIp.getAddress());
		log.debug("Use IP " + selectedIp + " hashed as " + UuidBinaryUtils.toHexString(digest) + " as node id");
		byte[] nodeId = NodeIdSupplier.toNodeIdBytes(digest, 0);
		// marks that this is not based on MAC address
		NodeIdSupplier.forceToNoMacAddress(nodeId, 0);
		return nodeId;
	}
}
