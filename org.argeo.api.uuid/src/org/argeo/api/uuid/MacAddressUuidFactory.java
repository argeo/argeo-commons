package org.argeo.api.uuid;

public class MacAddressUuidFactory extends ConcurrentUuidFactory {
	public final static UuidFactory DEFAULT = new MacAddressUuidFactory();

	public MacAddressUuidFactory() {
		setNodeIdSupplier(() -> {
			byte[] hardwareAddress = getHardwareAddress();
			byte[] macAddressNodeId = toNodeIdBytes(hardwareAddress, 0);
			long nodeIdBase = NodeIdSupplier.toNodeIdBase(macAddressNodeId);
			return nodeIdBase;
		});
	}

}
