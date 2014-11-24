package org.argeo.cms;

import javax.jcr.Item;

/** Convert from/to data layer to/from presentation layer. */
public interface TextInterpreter {
	public String raw(Item item);

	public String read(Item item);

	public void write(Item item, String content);
}
