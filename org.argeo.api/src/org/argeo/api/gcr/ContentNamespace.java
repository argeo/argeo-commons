package org.argeo.api.gcr;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * An XML-style namespace with a related UUID v3.
 * 
 * @see https://www.w3.org/TR/xml-names/
 */
public class ContentNamespace {
	private final UUID uuid;
	private final URI uri;

	public ContentNamespace(String uri) {
		try {
			this.uri = new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot interpret " + uri + " as URI", e);
		}
		this.uuid = namespaceUuid(this.uri);
	}

	public ContentNamespace(URI uri) {
		this.uri = uri;
		this.uuid = namespaceUuid(uri);
	}

	ContentNamespace(URI uri, UUID uuid) {
		this.uri = uri;
		assert uuid.equals(namespaceUuid(uri));
		this.uuid = uuid;
	}

	/** Empty namespace */
	private ContentNamespace() {
		try {
			this.uri = new URI("");
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot create empty URI");
		}
		this.uuid = NIL_UUID;
	}

	public UUID getUuid() {
		return uuid;
	}

	public URI getUri() {
		return uri;
	}

	public UUID nameUuid(String name) {
		return nameUuid(getUuid(), name);
	}


	public final static UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	public final static ContentNamespace EMPTY_NS = new ContentNamespace();

	/**
	 * The UUID v3 of http://www.w3.org/2000/xmlns/ within the standard DNS
	 * namespace, to be used as a base for the namespaces.
	 * 
	 * @see https://www.w3.org/TR/xml-names/#ns-decl
	 */
	// uuidgen --md5 --namespace @dns --name http://www.w3.org/2000/xmlns/
	// NOTE : must be declared before default namespaces
	public final static UUID XMLNS_UUID = UUID.fromString("4b352aad-ba1c-3139-b9d3-41e5816f6088");
	public final static ContentNamespace CR_NS = new ContentNamespace("http://argeo.org/ns/cr");


	public static UUID namespaceUuid(URI namespaceUri) {
		Objects.requireNonNull(namespaceUri, "Namespace URI cannot be null");
		return nameUUIDv3(XMLNS_UUID, namespaceUri.toString().getBytes(StandardCharsets.UTF_8));
	}

	public static UUID nameUuid(UUID namespace, String name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(namespace, "Name cannot be null");
		return nameUUIDv3(namespace, name.getBytes(StandardCharsets.UTF_8));
	}

	/*
	 * CANONICAL IMPLEMENTATION based on java.util.UUID.nameUUIDFromBytes(byte[])
	 */
	private static UUID nameUUIDv3(UUID namespace, byte[] name) {
		byte[] arr = new byte[name.length + 16];
		copyUuidBytes(namespace, arr, 0);
		System.arraycopy(name, 0, arr, 16, name.length);
		return UUID.nameUUIDFromBytes(arr);
	}

	private static void copyUuidBytes(UUID uuid, byte[] arr, int offset) {
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		assert arr.length >= 16 + offset;
		for (int i = offset; i < 8 + offset; i++)
			arr[i] = (byte) ((msb >> ((7 - i) * 8)) & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			arr[i] = (byte) ((lsb >> ((15 - i) * 8)) & 0xff);
	}
}
