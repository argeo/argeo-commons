package org.argeo.util.naming;

import javax.xml.namespace.QName;

/** A (possibly) qualified name. To be used in enums. */
public interface QNamed {
	String name();

	default QName qName() {
		return new DisplayQName(getNamespace(), name(), getDefaultPrefix());
	}

	String getNamespace();

	String getDefaultPrefix();

	static class DisplayQName extends QName {
		private static final long serialVersionUID = 2376484886212253123L;

		public DisplayQName(String namespaceURI, String localPart, String prefix) {
			super(namespaceURI, localPart, prefix);
		}

		public DisplayQName(String localPart) {
			super(localPart);
		}

		@Override
		public String toString() {
			String prefix = getPrefix();
			assert prefix != null;
			return "".equals(prefix) ? getLocalPart() : prefix + ":" + getLocalPart();
		}

	}
}
