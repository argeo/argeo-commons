package org.argeo.util.tabular;

import java.util.List;

/** Minimal tabular row wrapping an {@link Object} array */
public class ArrayTabularRow implements TabularRow {
	private final Object[] arr;

	public ArrayTabularRow(List<?> objs) {
		this.arr = objs.toArray();
	}

	public Object get(Integer col) {
		return arr[col];
	}

	public int size() {
		return arr.length;
	}

	public Object[] toArray() {
		return arr;
	}

}
