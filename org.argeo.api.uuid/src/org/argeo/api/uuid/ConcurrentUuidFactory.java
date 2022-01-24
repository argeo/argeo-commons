package org.argeo.api.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * A configurable implementation of an {@link AsyncUuidFactory}, which can be
 * used as a base class for more optimised implementations.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class ConcurrentUuidFactory extends AbstractAsyncUuidFactory {
	private final static Logger logger = System.getLogger(ConcurrentUuidFactory.class.getName());

//	private byte[] defaultNodeId;

	private Long nodeIdBase;

	public ConcurrentUuidFactory(byte[] nodeId, int offset) {
		Objects.requireNonNull(nodeId);
		if (offset + 6 > nodeId.length)
			throw new IllegalArgumentException("Offset too big: " + offset);
		byte[] defaultNodeId = toNodeIdBytes(nodeId, offset);
		nodeIdBase = NodeIdSupplier.toNodeIdBase(defaultNodeId);
		setNodeIdSupplier(() -> nodeIdBase);
	}

	protected ConcurrentUuidFactory() {

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
//
//	@Override
//	public UUID newTimeUUID() {
//		return newTimeUUID(timeUuidState.useTimestamp(), timeUuidState.getClockSequence(), defaultNodeId, 0);
//	}
}
