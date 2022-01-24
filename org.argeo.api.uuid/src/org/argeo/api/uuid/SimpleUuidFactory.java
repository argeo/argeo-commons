package org.argeo.api.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * Simple implementation of an {@link UuidFactory}, which can be used as a base
 * class for more optimised implementations.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class SimpleUuidFactory extends AbstractAsyncUuidFactory {
	private final static Logger logger = System.getLogger(SimpleUuidFactory.class.getName());
	public final static UuidFactory DEFAULT = new SimpleUuidFactory(null, -1, null);

//	private NodeId macAddressNodeId;
//	private NodeId defaultNodeId;
	private byte[] macAddressNodeId;
	private byte[] defaultNodeId;

	public SimpleUuidFactory(byte[] nodeId, int offset, Clock clock) {
		byte[] hardwareAddress = getHardwareAddress();
//		macAddressNodeId = hardwareAddress != null ? new NodeId(hardwareAddress, 0) : null;
		macAddressNodeId = toNodeId(hardwareAddress, 0);

//		defaultNodeId = nodeId != null ? new NodeId(nodeId, offset) : macAddressNodeId;
		defaultNodeId = nodeId != null ? toNodeId(nodeId, offset) : toNodeId(macAddressNodeId, 0);
		if (defaultNodeId == null)
			throw new IllegalStateException("No default node id specified");
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

	/*
	 * TIME-BASED (version 1)
	 */

	@Override
	public UUID newTimeUUIDwithMacAddress() {
		if (macAddressNodeId == null)
			throw new UnsupportedOperationException("No MAC address is available");
		return newTimeUUID(timeUuidState.useTimestamp(), timeUuidState.getClockSequence(), macAddressNodeId, 0);
	}

	@Override
	public UUID newTimeUUID() {
		return newTimeUUID(timeUuidState.useTimestamp(), timeUuidState.getClockSequence(), defaultNodeId, 0);
	}

	/*
	 * RANDOM v4
	 */
//	@Override
//	public UUID randomUUID(Random random) {
//		return newRandomUUID(random);
//	}

//	@Override
//	public UUID randomUUID() {
//		return randomUUID(secureRandom);
//	}
//
//	static class NodeId extends ThreadLocal<byte[]> {
//		private byte[] source;
//		private int offset;
//
//		public NodeId(byte[] source, int offset) {
//			Objects.requireNonNull(source);
//			this.source = source;
//			this.offset = offset;
//			if (offset < 0 || offset + 6 > source.length)
//				throw new ArrayIndexOutOfBoundsException(offset);
//		}
//
//		@Override
//		protected byte[] initialValue() {
//			byte[] value = new byte[6];
//			System.arraycopy(source, offset, value, 0, 6);
//			return value;
//		}
//
//	}
}
