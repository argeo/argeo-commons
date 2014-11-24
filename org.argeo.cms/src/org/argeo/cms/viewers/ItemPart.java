package org.argeo.cms.viewers;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/** An editable part related to a JCR Item */
public interface ItemPart<T extends Item> {
	public Item getItem() throws RepositoryException;
}
