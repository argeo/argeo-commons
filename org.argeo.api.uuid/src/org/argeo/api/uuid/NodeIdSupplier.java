package org.argeo.api.uuid;

import java.security.SecureRandom;
import java.util.function.Supplier;

/** A factory for node id base */
public interface NodeIdSupplier extends Supplier<Long> {
	static long toNodeIdBase(byte[] node) {
		assert node.length == 6;
		return UuidFactory.LEAST_SIG_RFC4122_VARIANT | (node[0] & 0xFFL) //
				| ((node[1] & 0xFFL) << 8) //
				| ((node[2] & 0xFFL) << 16) //
				| ((node[3] & 0xFFL) << 24) //
				| ((node[4] & 0xFFL) << 32) //
				| ((node[5] & 0xFFL) << 40); //
	}

	static boolean isNoMacAddressNodeId(byte[] nodeId) {
		return (nodeId[0] & 1) != 0;
	}

	static byte[] randomNodeId() {
		SecureRandom random = new SecureRandom();
		byte[] nodeId = new byte[6];
		random.nextBytes(nodeId);
		return nodeId;
	}

	/**
	 * Force this node id to be identified as no MAC address.
	 * 
	 * @see "https://datatracker.ietf.org/doc/html/rfc4122#section-4.5"
	 */
	static void forceToNoMacAddress(byte[] nodeId, int offset) {
		assert nodeId != null && offset < nodeId.length;
		nodeId[offset] = (byte) (nodeId[offset] | 1);
	}

	/*
	 * SPI UTILITIES
	 */
	/** Guarantees that a byte array of length 6 will be returned. */
	static byte[] toNodeIdBytes(byte[] source, int offset) {
		if (source == null)
			return null;
		if (offset < 0 || offset + 6 > source.length)
			throw new ArrayIndexOutOfBoundsException(offset);
		byte[] nodeId = new byte[6];
		System.arraycopy(source, offset, nodeId, 0, 6);
		return nodeId;
	}
}
