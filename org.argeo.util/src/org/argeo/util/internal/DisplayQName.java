package org.argeo.util.internal;

import javax.xml.namespace.QName;

public class DisplayQName extends QName {
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