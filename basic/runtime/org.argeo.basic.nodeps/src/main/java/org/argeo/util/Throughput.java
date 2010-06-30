/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.argeo.ArgeoException;

public class Throughput {
	private final static NumberFormat usNumberFormat = NumberFormat
			.getInstance(Locale.US);

	public enum Unit {
		s, m, h, d
	}

	private final Double value;
	private final Unit unit;

	public Throughput(Double value, Unit unit) {
		this.value = value;
		this.unit = unit;
	}

	public Throughput(Long periodMs, Long count, Unit unit) {
		if (unit.equals(Unit.s))
			value = ((double) count * 1000d) / periodMs;
		else if (unit.equals(Unit.m))
			value = ((double) count * 60d * 1000d) / periodMs;
		else if (unit.equals(Unit.h))
			value = ((double) count * 60d * 60d * 1000d) / periodMs;
		else if (unit.equals(Unit.d))
			value = ((double) count * 24d * 60d * 60d * 1000d) / periodMs;
		else
			throw new ArgeoException("Unsupported unit " + unit);
		this.unit = unit;
	}

	public Throughput(Double value, String unitStr) {
		this(value, Unit.valueOf(unitStr));
	}

	public Throughput(String def) {
		int index = def.indexOf('/');
		if (def.length() < 3 || index <= 0 || index != def.length() - 2)
			throw new ArgeoException(def + " no a proper throughput definition"
					+ " (should be <value>/<unit>, e.g. 3.54/s or 1500/h");
		String valueStr = def.substring(0, index);
		String unitStr = def.substring(index + 1);
		try {
			this.value = usNumberFormat.parse(valueStr).doubleValue();
		} catch (ParseException e) {
			throw new ArgeoException("Cannot parse " + valueStr
					+ " as a number.", e);
		}
		this.unit = Unit.valueOf(unitStr);
	}

	public Long asMsPeriod() {
		if (unit.equals(Unit.s))
			return Math.round(1000d / value);
		else if (unit.equals(Unit.m))
			return Math.round((60d * 1000d) / value);
		else if (unit.equals(Unit.h))
			return Math.round((60d * 60d * 1000d) / value);
		else if (unit.equals(Unit.d))
			return Math.round((24d * 60d * 60d * 1000d) / value);
		else
			throw new ArgeoException("Unsupported unit " + unit);
	}

	@Override
	public String toString() {
		return usNumberFormat.format(value) + '/' + unit;
	}

	public Double getValue() {
		return value;
	}

	public Unit getUnit() {
		return unit;
	}

}
