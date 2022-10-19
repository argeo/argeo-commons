package org.argeo.util.transaction;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import javax.transaction.xa.Xid;

/**
 * Implementation of {@link Xid} based on {@link UUID}, using max significant
 * bits as global transaction id, and least significant bits as branch
 * qualifier.
 */
public class UuidXid implements Xid, Serializable {
	private static final long serialVersionUID = -5380531989917886819L;
	public final static int FORMAT = (int) serialVersionUID;

	private final static int BYTES_PER_LONG = Long.SIZE / Byte.SIZE;

	private final int format;
	private final byte[] globalTransactionId;
	private final byte[] branchQualifier;
	private final String uuid;
	private final int hashCode;

	public UuidXid() {
		this(UUID.randomUUID());
	}

	public UuidXid(UUID uuid) {
		this.format = FORMAT;
		this.globalTransactionId = uuidToBytes(uuid.getMostSignificantBits());
		this.branchQualifier = uuidToBytes(uuid.getLeastSignificantBits());
		this.uuid = uuid.toString();
		this.hashCode = uuid.hashCode();
	}

	public UuidXid(Xid xid) {
		this(xid.getFormatId(), xid.getGlobalTransactionId(), xid
				.getBranchQualifier());
	}

	private UuidXid(int format, byte[] globalTransactionId,
			byte[] branchQualifier) {
		this.format = format;
		this.globalTransactionId = globalTransactionId;
		this.branchQualifier = branchQualifier;
		this.uuid = bytesToUUID(globalTransactionId, branchQualifier)
				.toString();
		this.hashCode = uuid.hashCode();
	}

	@Override
	public int getFormatId() {
		return format;
	}

	@Override
	public byte[] getGlobalTransactionId() {
		return Arrays.copyOf(globalTransactionId, globalTransactionId.length);
	}

	@Override
	public byte[] getBranchQualifier() {
		return Arrays.copyOf(branchQualifier, branchQualifier.length);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof UuidXid) {
			UuidXid that = (UuidXid) obj;
			return Arrays.equals(globalTransactionId, that.globalTransactionId)
					&& Arrays.equals(branchQualifier, that.branchQualifier);
		}
		if (obj instanceof Xid) {
			Xid that = (Xid) obj;
			return Arrays.equals(globalTransactionId,
					that.getGlobalTransactionId())
					&& Arrays
							.equals(branchQualifier, that.getBranchQualifier());
		}
		return uuid.equals(obj.toString());
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new UuidXid(format, globalTransactionId, branchQualifier);
	}

	@Override
	public String toString() {
		return uuid;
	}

	public UUID asUuid() {
		return bytesToUUID(globalTransactionId, branchQualifier);
	}

	public static byte[] uuidToBytes(long bits) {
		ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_LONG);
		buffer.putLong(0, bits);
		return buffer.array();
	}

	public static UUID bytesToUUID(byte[] most, byte[] least) {
		if (most.length < BYTES_PER_LONG)
			most = Arrays.copyOf(most, BYTES_PER_LONG);
		if (least.length < BYTES_PER_LONG)
			least = Arrays.copyOf(least, BYTES_PER_LONG);
		ByteBuffer buffer = ByteBuffer.allocate(2 * BYTES_PER_LONG);
		buffer.put(most, 0, BYTES_PER_LONG);
		buffer.put(least, 0, BYTES_PER_LONG);
		buffer.flip();
		return new UUID(buffer.getLong(), buffer.getLong());
	}

	// public static void main(String[] args) {
	// UUID uuid = UUID.randomUUID();
	// System.out.println(uuid);
	// uuid = bytesToUUID(uuidToBytes(uuid.getMostSignificantBits()),
	// uuidToBytes(uuid.getLeastSignificantBits()));
	// System.out.println(uuid);
	// }
}
