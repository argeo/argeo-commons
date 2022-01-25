package org.argeo.api.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;

/**
 * A configurable implementation of an {@link AsyncUuidFactory}, which can be
 * used as a base class for more optimised implementations.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class ConcurrentUuidFactory extends AbstractAsyncUuidFactory {
	private final static Logger logger = System.getLogger(ConcurrentUuidFactory.class.getName());

	private Long nodeIdBase;

	public ConcurrentUuidFactory(byte[] nodeId, int offset) {
		Objects.requireNonNull(nodeId);
		if (offset + 6 > nodeId.length)
			throw new IllegalArgumentException("Offset too big: " + offset);
		byte[] defaultNodeId = toNodeIdBytes(nodeId, offset);
		nodeIdBase = NodeIdSupplier.toNodeIdBase(defaultNodeId);
		setNodeIdSupplier(() -> nodeIdBase);
		assert newTimeUUID().node() == BitSet.valueOf(defaultNodeId).toLongArray()[0];
	}

	public ConcurrentUuidFactory() {
		byte[] defaultNodeId = getIpBytes();
		nodeIdBase = NodeIdSupplier.toNodeIdBase(defaultNodeId);
		setNodeIdSupplier(() -> nodeIdBase);
		assert newTimeUUID().node() == BitSet.valueOf(defaultNodeId).toLongArray()[0];
	}

	/*
	 * DEFAULT
	 */
	/**
	 * The default {@link UUID} to provide. This implementations returns
	 * {@link #timeUUID()} because it is fast and uses few resources.
	 */
	@Override
	public UUID get() {
		return timeUUID();
	}

	@Override
	protected SecureRandom newSecureRandom() {
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance("DRBG",
					DrbgParameters.instantiation(256, DrbgParameters.Capability.PR_AND_RESEED, "UUID".getBytes()));
		} catch (NoSuchAlgorithmException e) {
			try {
				logger.log(DEBUG, "DRBG secure random not found, using strong");
				secureRandom = SecureRandom.getInstanceStrong();
			} catch (NoSuchAlgorithmException e1) {
				logger.log(WARNING, "No strong secure random was found, using default");
				secureRandom = new SecureRandom();
			}
		}
		return secureRandom;
	}

	/** Returns an SHA1 digest of one of the IP addresses. */
	protected byte[] getIpBytes() {
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
		logger.log(INFO, "Use IP " + selectedIp + " hashed as " + toHexString(digest) + " as node id");
		byte[] nodeId = toNodeIdBytes(digest, 0);
		// marks that this is not based on MAC address
		forceToNoMacAddress(nodeId, 0);
		return nodeId;
	}
}