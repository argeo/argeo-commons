package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.argeo.api.acr.StructuredData;

public class CLine<K extends Enum<K>> extends EnumMap<K, Object> implements StructuredData<K, Object, String> {
	private static final long serialVersionUID = -4017309115985186722L;

	private List<String> plainArgs = new ArrayList<>();

	CLine(Class<K> keyType) {
		super(keyType);
	}

	@Override
	public Iterator<String> iterator() {
		return plainArgs.iterator();
	}

	void addPlainArg(String arg) {
		plainArgs.add(arg);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A> Optional<A> get(K key, Class<A> clss) {
		if (!containsKey(key))
			return Optional.empty();
		Object value = get(key);
		Objects.requireNonNull(value);
		if (clss.isAssignableFrom(value.getClass()))
			return Optional.of((A) value);
		if (clss.isAssignableFrom(String.class))
			return Optional.of((A) value.toString());
		throw new IllegalArgumentException(
				"Cannot convert attribute " + key + " with value " + value.getClass() + " to " + clss);
	}

	@Override
	public Class<? extends Object> getType(K key) {
		return String.class;
	}

	public List<String> getPlainArgs() {
		return Collections.unmodifiableList(plainArgs);
	}

}
