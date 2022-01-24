package org.argeo.api.uuid;

import java.util.function.Supplier;

/** A factory for node id base */
public interface NodeIdSupplier extends Supplier<Long> {
	long LEAST_SIG_RFC4122_VARIANT = (1l << 63);

	static long toNodeIdBase(byte[] node) {
		assert node.length == 6;
		return LEAST_SIG_RFC4122_VARIANT // base for Leach–Salz UUID
				| (node[0] & 0xFFL) //
				| ((node[1] & 0xFFL) << 8) //
				| ((node[2] & 0xFFL) << 16) //
				| ((node[3] & 0xFFL) << 24) //
				| ((node[4] & 0xFFL) << 32) //
				| ((node[5] & 0xFFL) << 40); //
	}

}
