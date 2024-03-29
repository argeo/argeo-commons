package org.argeo.api.cms.ux;

import java.util.function.BiFunction;

/**
 * Stateless UI part creator. Takes a parent view (V) and a model context (M) in
 * order to create a view part (W) which can then be further configured. Such
 * object can be used as services and reference other part of the model which
 * are relevant for all created UI part.
 */
@FunctionalInterface
@Deprecated
public interface MvcProvider<V, M, W> extends BiFunction<V, M, W> {
	W createUiPart(V parent, M context);
	
	/**
	 * Whether this parent view is supported.
	 * 
	 * @return true by default.
	 */
	default boolean isViewSupported(V parent) {
		return true;
	}

	/**
	 * Whether this context is supported.
	 * 
	 * @return true by default.
	 */
	default boolean isModelSupported(M context) {
		return true;
	}

	default W apply(V parent, M context) {
		if (!isViewSupported(parent))
			throw new IllegalArgumentException("Parent view " + parent + "is not supported.");
		if (!isModelSupported(context))
			throw new IllegalArgumentException("Model context " + context + "is not supported.");
		return createUiPart(parent, context);
	}

	default W createUiPart(V parent) {
		return createUiPart(parent, null);
	}
}
