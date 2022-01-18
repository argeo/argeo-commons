package org.argeo.api.gcr;

import java.util.UUID;

public interface ContentName {
	UUID getUuid();

	ContentNamespace getNamespace();

	String getName();

	static boolean contains(ContentName[] classes, ContentName name) {
		for (ContentName clss : classes) {
			if (clss.getUuid().equals(name.getUuid()))
				return true;
		}
		return false;
	}
}
