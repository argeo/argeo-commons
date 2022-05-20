package org.argeo.util.register;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ComponentRegister extends Consumer<Component<?>> {
	<T> Component<? extends T> find(Class<T> clss, Predicate<Map<String, Object>> filter);

	Component<?> get(Object instance);

	void activate();

	void deactivate();

	boolean isActive();

	void clear();
}
