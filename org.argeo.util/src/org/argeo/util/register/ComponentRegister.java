package org.argeo.util.register;

import java.util.Map;
import java.util.SortedSet;
import java.util.function.Predicate;

/** A register of components which can coordinate their activation. */
public interface ComponentRegister {
	long register(Component<?> component);

	<T> SortedSet<Component<? extends T>> find(Class<T> clss, Predicate<Map<String, Object>> filter);

	Component<?> get(Object instance);

//	default <T> PublishedType<T> getType(Class<T> clss) {
//		SortedSet<Component<? extends T>> components = find(clss, null);
//		if (components.size() == 0)
//			return null;
//		return components.first().getType(clss);
//	}

	void activate();

	void deactivate();

	boolean isActive();

	void clear();
}
