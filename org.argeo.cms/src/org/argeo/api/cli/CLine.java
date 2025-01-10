package org.argeo.api.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.argeo.api.acr.CrAttributeType;

public class CLine {
	private Map<Class<? extends Enum<?>>, EnumMap<? extends Enum<?>, Object>> options = new HashMap<>();
	private List<String> plainArgs = new ArrayList<>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	CLine(List<Class<? extends Enum<?>>> optEnums) {
		for (Class<? extends Enum<?>> clss : optEnums) {
			// just an assert as runtime check is performed by parser
			assert !options.containsKey(clss);
			options.put(clss, new EnumMap(clss));
		}
	}

	void addPlainArg(String arg) {
		plainArgs.add(arg);
	}

	EnumMap<? extends Enum<?>, Object> getEnumMap(Class<? extends Enum<?>> clss) {
		if (!options.containsKey(clss))
			throw new IllegalArgumentException("Options enum " + clss + " not available");
		EnumMap<? extends Enum<?>, Object> res = options.get(clss);
		assert res != null;
		return res;
	}

	@SuppressWarnings("unchecked")
	<T extends Enum<T>> void put(Enum<T> opt, Object value) {
		Class<? extends Enum<?>> clss = (Class<? extends Enum<?>>) opt.getClass();
		EnumMap<T, Object> map = (EnumMap<T, Object>) getEnumMap(clss);
		map.put((T) opt, value);
	}

	public <A> Optional<A> get(Enum<?> key, Class<A> clss) {
		EnumMap<? extends Enum<?>, Object> opts = null;
		optionEnums: for (Class<? extends Enum<?>> c : options.keySet()) {
			if (c.isAssignableFrom(key.getClass())) {
				opts = options.get(c);
				break optionEnums;
			}
		}
		if (opts == null)
			throw new IllegalArgumentException(key.getClass() + " options not supported by this commend line");
		if (!opts.containsKey(key))
			return Optional.empty();
		Object value = opts.get(key);
		Objects.requireNonNull(value);
		return CrAttributeType.cast(clss, value);
//		if (clss.isAssignableFrom(value.getClass()))
//			return Optional.of((A) value);
//		if (clss.isAssignableFrom(String.class))
//			return Optional.of((A) value.toString());
//		throw new IllegalArgumentException(
//				"Cannot convert attribute " + key + " with value " + value.getClass() + " to " + clss);
	}

	public boolean has(Enum<?> opt) {
		return get(opt, Boolean.class).orElse(false);
	}

	public List<String> getPlainArgs() {
		return Collections.unmodifiableList(plainArgs);
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(",");
		for (EnumMap<? extends Enum<?>, Object> map : options.values()) {
			if (!map.isEmpty())
				sj.add(map.toString());
		}
		sj.add(plainArgs.toString());
		return sj.toString();
	}

}
