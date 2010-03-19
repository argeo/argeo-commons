package org.argeo.util;

import junit.framework.TestCase;

public class ThroughputTest extends TestCase {
	public void testParse() {
		Throughput t;
		t = new Throughput("3.54/s");
		assertEquals(3.54d, t.getValue());
		assertEquals(Throughput.Unit.s, t.getUnit());
		assertEquals(282l, (long) t.asMsPeriod());

		t = new Throughput("35698.2569/h");
		assertEquals(Throughput.Unit.h, t.getUnit());
		assertEquals(101l, (long) t.asMsPeriod());
	}
}
