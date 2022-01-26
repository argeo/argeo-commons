package org.argeo.api.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * A configurable implementation of an {@link AsyncUuidFactory}, which can be
 * used as a base class for more optimised implementations.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class ConcurrentUuidFactory extends AbstractAsyncUuidFactory implements TypedUuidFactory {
	private final static Logger logger = System.getLogger(ConcurrentUuidFactory.class.getName());

	public ConcurrentUuidFactory(byte[] nodeId) {
		this(nodeId, 0);
	}

	public ConcurrentUuidFactory(byte[] nodeId, int offset) {
		Objects.requireNonNull(nodeId);
		if (offset + 6 > nodeId.length)
			throw new IllegalArgumentException("Offset too big: " + offset);
		byte[] defaultNodeId = toNodeIdBytes(nodeId, offset);
		long nodeIdBase = NodeIdSupplier.toNodeIdBase(defaultNodeId);
		setNodeIdSupplier(() -> nodeIdBase);
	}

	/**
	 * Empty constructor for use with component life cycle. A {@link NodeIdSupplier}
	 * must be set externally, otherwise time based UUID won't work.
	 */
	public ConcurrentUuidFactory() {
		super();
	}

//	public ConcurrentUuidFactory() {
//		byte[] defaultNodeId = getIpBytes();
//		nodeIdBase = NodeIdSupplier.toNodeIdBase(defaultNodeId);
//		setNodeIdSupplier(() -> nodeIdBase);
//		assert newTimeUUID().node() == BitSet.valueOf(defaultNodeId).toLongArray()[0];
//	}

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
	protected SecureRandom createSecureRandom() {
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

}