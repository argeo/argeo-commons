package org.argeo.osgi.useradmin;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.argeo.osgi.useradmin.AbstractUserDirectory.WorkingCopy;

/** Directory user implementation */
class LdifUser implements DirectoryUser {
	private final AbstractUserDirectory userAdmin;

	private final LdapName dn;

	private final boolean frozen;
	private Attributes publishedAttributes;

	private final AttributeDictionary properties;
	private final AttributeDictionary credentials;

	LdifUser(AbstractUserDirectory userAdmin, LdapName dn, Attributes attributes) {
		this(userAdmin, dn, attributes, false);
	}

	private LdifUser(AbstractUserDirectory userAdmin, LdapName dn,
			Attributes attributes, boolean frozen) {
		this.userAdmin = userAdmin;
		this.dn = dn;
		this.publishedAttributes = attributes;
		properties = new AttributeDictionary(false);
		credentials = new AttributeDictionary(true);
		this.frozen = frozen;
	}

	@Override
	public String getName() {
		return dn.toString();
	}

	@Override
	public int getType() {
		return USER;
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	@Override
	public Dictionary<String, Object> getCredentials() {
		return credentials;
	}

	@Override
	public boolean hasCredential(String key, Object value) {
		if (key == null) {
			// TODO check other sources (like PKCS12)
			char[] password = toChars(value);
			byte[] hashedPassword = hash(password);
			return hasCredential(LdifName.userpassword.name(), hashedPassword);
		}

		Object storedValue = getCredentials().get(key);
		if (storedValue == null || value == null)
			return false;
		if (!(value instanceof String || value instanceof byte[]))
			return false;
		if (storedValue instanceof String && value instanceof String)
			return storedValue.equals(value);
		if (storedValue instanceof byte[] && value instanceof byte[])
			return Arrays.equals((byte[]) storedValue, (byte[]) value);
		return false;
	}

	/** Hash and clear the password */
	private byte[] hash(char[] password) {
		byte[] hashedPassword = ("{SHA}" + Base64
				.encodeBase64String(DigestUtils.sha1(toBytes(password))))
				.getBytes();
		Arrays.fill(password, '\u0000');
		return hashedPassword;
	}

	private byte[] toBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
				byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	private char[] toChars(Object obj) {
		if (obj instanceof char[])
			return (char[]) obj;
		if (!(obj instanceof byte[]))
			throw new IllegalArgumentException(obj.getClass()
					+ " is not a byte array");
		ByteBuffer fromBuffer = ByteBuffer.wrap((byte[]) obj);
		CharBuffer toBuffer = Charset.forName("UTF-8").decode(fromBuffer);
		char[] res = Arrays.copyOfRange(toBuffer.array(), toBuffer.position(),
				toBuffer.limit());
		Arrays.fill(fromBuffer.array(), (byte) 0); // clear sensitive data
		Arrays.fill((byte[]) obj, (byte) 0); // clear sensitive data
		Arrays.fill(toBuffer.array(), '\u0000'); // clear sensitive data
		return res;
	}

	@Override
	public LdapName getDn() {
		return dn;
	}

	@Override
	public synchronized Attributes getAttributes() {
		return isEditing() ? getModifiedAttributes() : publishedAttributes;
	}

	/** Should only be called from working copy thread. */
	private synchronized Attributes getModifiedAttributes() {
		assert getWc() != null;
		return getWc().getAttributes(getDn());
	}

	protected synchronized boolean isEditing() {
		return getWc() != null && getModifiedAttributes() != null;
	}

	private synchronized WorkingCopy getWc() {
		return userAdmin.getWorkingCopy();
	}

	protected synchronized void startEditing() {
		if (frozen)
			throw new UserDirectoryException("Cannot edit frozen view");
		if (getUserAdmin().isReadOnly())
			throw new UserDirectoryException("User directory is read-only");
		assert getModifiedAttributes() == null;
		getWc().startEditing(this);
		// modifiedAttributes = (Attributes) publishedAttributes.clone();
	}

	public synchronized void publishAttributes(Attributes modifiedAttributes) {
		publishedAttributes = modifiedAttributes;
	}

	// protected synchronized void stopEditing(boolean apply) {
	// assert getModifiedAttributes() != null;
	// if (apply)
	// publishedAttributes = getModifiedAttributes();
	// // modifiedAttributes = null;
	// }

	public DirectoryUser getPublished() {
		return new LdifUser(userAdmin, dn, publishedAttributes, true);
	}

	@Override
	public int hashCode() {
		return dn.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof LdifUser) {
			LdifUser that = (LdifUser) obj;
			return this.dn.equals(that.dn);
		}
		return false;
	}

	@Override
	public String toString() {
		return dn.toString();
	}

	protected AbstractUserDirectory getUserAdmin() {
		return userAdmin;
	}

	private class AttributeDictionary extends Dictionary<String, Object> {
		private final List<String> effectiveKeys = new ArrayList<String>();
		private final List<String> attrFilter;
		private final Boolean includeFilter;

		public AttributeDictionary(Boolean includeFilter) {
			this.attrFilter = userAdmin.getCredentialAttributeIds();
			this.includeFilter = includeFilter;
			try {
				NamingEnumeration<String> ids = getAttributes().getIDs();
				while (ids.hasMore()) {
					String id = ids.next();
					if (includeFilter && attrFilter.contains(id))
						effectiveKeys.add(id);
					else if (!includeFilter && !attrFilter.contains(id))
						effectiveKeys.add(id);
				}
			} catch (NamingException e) {
				throw new UserDirectoryException(
						"Cannot initialise attribute dictionary", e);
			}
		}

		@Override
		public int size() {
			return effectiveKeys.size();
		}

		@Override
		public boolean isEmpty() {
			return effectiveKeys.size() == 0;
		}

		@Override
		public Enumeration<String> keys() {
			return Collections.enumeration(effectiveKeys);
		}

		@Override
		public Enumeration<Object> elements() {
			final Iterator<String> it = effectiveKeys.iterator();
			return new Enumeration<Object>() {

				@Override
				public boolean hasMoreElements() {
					return it.hasNext();
				}

				@Override
				public Object nextElement() {
					String key = it.next();
					try {
						return getAttributes().get(key).get();
					} catch (NamingException e) {
						throw new UserDirectoryException(
								"Cannot get value for key " + key, e);
					}
				}

			};
		}

		@Override
		public Object get(Object key) {
			try {
				Attribute attr = getAttributes().get(key.toString());
				if (attr == null)
					return null;
				return attr.get();
			} catch (NamingException e) {
				throw new UserDirectoryException(
						"Cannot get value for attribute " + key, e);
			}
		}

		@Override
		public Object put(String key, Object value) {
			if (key == null) {
				// TODO persist to other sources (like PKCS12)
				char[] password = toChars(value);
				byte[] hashedPassword = hash(password);
				return put(LdifName.userpassword.name(), hashedPassword);
			}

			userAdmin.checkEdit();
			if (!isEditing())
				startEditing();

			if (!(value instanceof String || value instanceof byte[]))
				throw new IllegalArgumentException(
						"Value must be String or byte[]");

			if (includeFilter && !attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key
						+ " not included");
			else if (!includeFilter && attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key + " excluded");

			try {
				Attribute attribute = getModifiedAttributes().get(
						key.toString());
				attribute = new BasicAttribute(key.toString());
				attribute.add(value);
				Attribute previousAttribute = getModifiedAttributes().put(
						attribute);
				if (previousAttribute != null)
					return previousAttribute.get();
				else
					return null;
			} catch (NamingException e) {
				throw new UserDirectoryException(
						"Cannot get value for attribute " + key, e);
			}
		}

		@Override
		public Object remove(Object key) {
			userAdmin.checkEdit();
			if (!isEditing())
				startEditing();

			if (includeFilter && !attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key
						+ " not included");
			else if (!includeFilter && attrFilter.contains(key))
				throw new IllegalArgumentException("Key " + key + " excluded");

			try {
				Attribute attr = getModifiedAttributes().remove(key.toString());
				if (attr != null)
					return attr.get();
				else
					return null;
			} catch (NamingException e) {
				throw new UserDirectoryException("Cannot remove attribute "
						+ key, e);
			}
		}
	}

}
