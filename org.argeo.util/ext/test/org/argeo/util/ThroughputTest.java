/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
