package org.argeo.util;

public class ThroughputTest {
	public void testParse() throws Exception {
//		assert 0 == 1;

		Throughput t;
		t = new Throughput("3.54/s");
		assert 3.54d == t.getValue();
		assert Throughput.Unit.s.equals(t.getUnit());
		assert 282l == (long) t.asMsPeriod();

		t = new Throughput("35698.2569/h");
		assert Throughput.Unit.h.equals(t.getUnit());
		assert 101l == (long) t.asMsPeriod();
	}
}
