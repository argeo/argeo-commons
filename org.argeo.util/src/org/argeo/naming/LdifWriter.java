package org.argeo.naming;

import static org.argeo.naming.LdapAttrs.DN;
import static org.argeo.naming.LdapAttrs.member;
import static org.argeo.naming.LdapAttrs.objectClass;
import static org.argeo.naming.LdapAttrs.uniqueMember;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.osgi.useradmin.UserDirectoryException;

/** Basic LDIF writer */
public class LdifWriter {
	private final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private final Writer writer;

	/** Writer must be closed by caller */
	public LdifWriter(Writer writer) {
		this.writer = writer;
	}

	/** Stream must be closed by caller */
	public LdifWriter(OutputStream out) {
		this(new OutputStreamWriter(out, DEFAULT_CHARSET));
	}

	public void writeEntry(LdapName name, Attributes attributes) throws IOException {
		try {
			// check consistency
			Rdn nameRdn = name.getRdn(name.size() - 1);
			Attribute nameAttr = attributes.get(nameRdn.getType());
			if (!nameAttr.get().equals(nameRdn.getValue()))
				throw new UserDirectoryException(
						"Attribute " + nameAttr.getID() + "=" + nameAttr.get() + " not consistent with DN " + name);

			writer.append(DN + ": ").append(name.toString()).append('\n');
			Attribute objectClassAttr = attributes.get(objectClass.name());
			if (objectClassAttr != null)
				writeAttribute(objectClassAttr);
			attributes: for (NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMore();) {
				Attribute attribute = attrs.next();
				if (attribute.getID().equals(DN) || attribute.getID().equals(objectClass.name()))
					continue attributes;// skip DN attribute
				if (attribute.getID().equals(member.name()) || attribute.getID().equals(uniqueMember.name()))
					continue attributes;// skip member and uniqueMember attributes, so that they are always written last
				writeAttribute(attribute);
			}
			// write member and uniqueMember attributes last
			for (NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMore();) {
				Attribute attribute = attrs.next();
				if (attribute.getID().equals(member.name()) || attribute.getID().equals(uniqueMember.name()))
					writeMemberAttribute(attribute);
			}
			writer.append('\n');
			writer.flush();
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot write LDIF", e);
		}
	}

	public void write(Map<LdapName, Attributes> entries) throws IOException {
		for (LdapName dn : entries.keySet())
			writeEntry(dn, entries.get(dn));
	}

	protected void writeAttribute(Attribute attribute) throws NamingException, IOException {
		for (NamingEnumeration<?> attrValues = attribute.getAll(); attrValues.hasMore();) {
			Object value = attrValues.next();
			if (value instanceof byte[]) {
				String encoded = Base64.getEncoder().encodeToString((byte[]) value);
				writer.append(attribute.getID()).append(":: ").append(encoded).append('\n');
			} else {
				writer.append(attribute.getID()).append(": ").append(value.toString()).append('\n');
			}
		}
	}

	protected void writeMemberAttribute(Attribute attribute) throws NamingException, IOException {
		// Note: duplicate entries will be swallowed
		SortedSet<String> values = new TreeSet<>();
		for (NamingEnumeration<?> attrValues = attribute.getAll(); attrValues.hasMore();) {
			String value = attrValues.next().toString();
			values.add(value);
		}

		for (String value : values) {
			writer.append(attribute.getID()).append(": ").append(value).append('\n');
		}
	}
}
