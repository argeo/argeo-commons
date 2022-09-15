package org.argeo.cms.dav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class DavPropfind {
	private DavDepth depth;
	private boolean propname = false;
	private boolean allprop = false;
	private List<QName> props = new ArrayList<>();

	public DavPropfind(DavDepth depth) {
		this.depth = depth;
	}

	public boolean isPropname() {
		return propname;
	}

	public void setPropname(boolean propname) {
		this.propname = propname;
	}

	public boolean isAllprop() {
		return allprop;
	}

	public void setAllprop(boolean allprop) {
		this.allprop = allprop;
	}

	public List<QName> getProps() {
		return props;
	}

	public DavDepth getDepth() {
		return depth;
	}

	public static DavPropfind load(DavDepth depth, InputStream in) throws IOException {
		try {
			DavPropfind res = null;
			XMLInputFactory inputFactory = XMLInputFactory.newFactory();
			XMLStreamReader reader = inputFactory.createXMLStreamReader(in);
			while (reader.hasNext()) {
				reader.next();
				if (reader.isStartElement()) {
					QName name = reader.getName();
//		System.out.println(name);
					DavXmlElement davXmlElement = DavXmlElement.toEnum(name);
					if (davXmlElement != null) {
						switch (davXmlElement) {
						case propfind:
							res = new DavPropfind(depth);
							break;
						case allprop:
							res.setAllprop(true);
							break;
						case propname:
							res.setPropname(true);
						case prop:
							// ignore
						case include:
							// ignore
							break;
						default:
							// TODO check that the format is really respected
							res.getProps().add(reader.getName());
						}
					}
				}
			}

			// checks
			if (res.isPropname()) {
				if (!res.getProps().isEmpty() || res.isAllprop())
					throw new IllegalArgumentException("Cannot set other values if propname is set");
			}
			return res;
		} catch (FactoryConfigurationError | XMLStreamException e) {
			throw new RuntimeException("Cannot load propfind", e);
		}
	}
}
