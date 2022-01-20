package org.argeo.api.gcr;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * A {@link QName} which MUST have prefix and whose {@link #toString()} method
 * returns the prefixed form (prefix:localPart).
 */
public class ContentName extends QName {
	private static final long serialVersionUID = 5722920985400306100L;
	public final static UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	/**
	 * The UUID v3 of http://www.w3.org/2000/xmlns/ within the standard DNS
	 * namespace, to be used as a base for the namespaces.
	 * 
	 * @see https://www.w3.org/TR/xml-names/#ns-decl
	 */
	// uuidgen --md5 --namespace @dns --name http://www.w3.org/2000/xmlns/
	// NOTE : must be declared before default namespaces
	public final static UUID XMLNS_UUID = UUID.fromString("4b352aad-ba1c-3139-b9d3-41e5816f6088");
	// uuidgen --md5 --namespace 4b352aad-ba1c-3139-b9d3-41e5816f6088 --name ""
	public final static UUID NULL_NS_UUID = UUID.fromString("f07726e3-99c8-3178-b758-a86ed41f300d");

	private final static Map<String, UUID> namespaceUuids = Collections.synchronizedMap(new TreeMap<>());
	private final static Map<String, UUID> nameUuids = Collections.synchronizedMap(new TreeMap<>());

	static {
		assert NULL_NS_UUID.equals(nameUUIDv3(XMLNS_UUID, XMLConstants.NULL_NS_URI.getBytes(UTF_8)));
	}

//	private final UUID uuid;

	public ContentName(String namespaceURI, String localPart, NamespaceContext nsContext) {
		this(namespaceURI, localPart, nsContext.getPrefix(namespaceURI));
	}

	protected ContentName(String namespaceURI, String localPart, String prefix) {
		super(namespaceURI, localPart, prefix);
		if (prefix == null)
			throw new IllegalArgumentException("Prefix annot be null");
	}

	public ContentName(String localPart) {
		this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX);
	}

	public ContentName(QName qName, NamespaceContext nsContext) {
		this(qName.getNamespaceURI(), qName.getLocalPart(), nsContext);
	}

	public String toQNameString() {
		return super.toString();
	}

	public String toPrefixedString() {
		return toPrefixedString(this);
	}

	/*
	 * OBJECT METHOS
	 */

	@Override
	public String toString() {
		return toPrefixedString();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new ContentName(getNamespaceURI(), getLocalPart(), getPrefix());
	}

	public static String toPrefixedString(QName name) {
		String prefix = name.getPrefix();
		assert prefix != null;
		return "".equals(prefix) ? name.getLocalPart() : prefix + ":" + name.getLocalPart();
	}
//	ContentNamespace getNamespace();
//
//	String getName();

	public static UUID namespaceUuid(String namespaceURI) {
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return NULL_NS_UUID;
		Objects.requireNonNull(namespaceURI, "Namespace URI cannot be null");
		synchronized (namespaceUuids) {
			UUID namespaceUuid = namespaceUuids.get(namespaceURI);
			if (namespaceUuid == null) {
				namespaceUuid = nameUUIDv3(ContentName.XMLNS_UUID,
						namespaceURI.toString().getBytes(StandardCharsets.UTF_8));
				namespaceUuids.put(namespaceURI, namespaceUuid);
			}
			return namespaceUuid;
		}
	}

	public static UUID nameUuid(String namespaceURI, QName name) {
		return nameUuid(name.getNamespaceURI(), name.getLocalPart());
	}

	public static UUID nameUuid(String namespaceURI, String name) {
		Objects.requireNonNull(namespaceURI, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");
		synchronized (nameUuids) {
			String key = XMLConstants.NULL_NS_URI.equals(namespaceURI) ? name : "{" + namespaceURI + "}" + name;
			UUID nameUuid = nameUuids.get(key);
			if (nameUuid == null) {
				UUID namespaceUuid = namespaceUuid(namespaceURI);
				nameUuid = nameUUIDv3(namespaceUuid, name.getBytes(StandardCharsets.UTF_8));
				namespaceUuids.put(key, nameUuid);
			}
			return nameUuid;
		}
	}

	/*
	 * CANONICAL IMPLEMENTATION based on java.util.UUID.nameUUIDFromBytes(byte[])
	 */
	static UUID nameUUIDv3(UUID namespace, byte[] name) {
		byte[] arr = new byte[name.length + 16];
		ContentName.copyUuidBytes(namespace, arr, 0);
		System.arraycopy(name, 0, arr, 16, name.length);
		return UUID.nameUUIDFromBytes(arr);
	}

	static void copyUuidBytes(UUID uuid, byte[] arr, int offset) {
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		assert arr.length >= 16 + offset;
		for (int i = offset; i < 8 + offset; i++)
			arr[i] = (byte) ((msb >> ((7 - i) * 8)) & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			arr[i] = (byte) ((lsb >> ((15 - i) * 8)) & 0xff);
	}

	/*
	 * UTILITIES
	 */

	public static boolean contains(QName[] classes, QName name) {
		for (QName clss : classes) {
			if (clss.equals(name))
				return true;
		}
		return false;
	}
}
