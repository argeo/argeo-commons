package org.argeo.api.cli;

import java.util.List;
import java.util.Optional;

public interface CLine {
	<A> Optional<A> get(Enum<?> key, Class<A> clss) throws IllegalArgumentException;

	<A> List<A> getMultiple(Enum<?> key, Class<A> clss);

	/** Whether this option is a boolean switch AND is true. */
	boolean flag(Enum<?> opt);

	List<String> getPlainArgs();
}
