package org.argeo.naming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Base64;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.argeo.osgi.useradmin.UserDirectoryException;

/** Basic LDIF writer */
public class LdifWriter {
	private final Writer writer;

	/** Writer must be closed by caller */
	public LdifWriter(Writer writer) {
		this.writer = writer;
	}

	/** Stream must be closed by caller */
	public LdifWriter(OutputStream out) {
		this(new OutputStreamWriter(out));
	}

	public void writeEntry(LdapName name, Attributes attributes) throws IOException {
		try {
			// check consistency
			Rdn nameRdn = name.getRdn(name.size() - 1);
			Attribute nameAttr = attributes.get(nameRdn.getType());
			if (!nameAttr.get().equals(nameRdn.getValue()))
				throw new UserDirectoryException(
						"Attribute " + nameAttr.getID() + "=" + nameAttr.get() + " not consistent with DN " + name);

			writer.append(LdapAttrs.DN + ":").append(name.toString()).append('\n');
			Attribute objectClassAttr = attributes.get("objectClass");
			if (objectClassAttr != null)
				writeAttribute(objectClassAttr);
			for (NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMore();) {
				Attribute attribute = attrs.next();
				if (attribute.getID().equals(LdapAttrs.DN) || attribute.getID().equals("objectClass"))
					continue;// skip DN attribute
				writeAttribute(attribute);
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
				writer.append(attribute.getID()).append("::").append(encoded).append('\n');
			} else {
				writer.append(attribute.getID()).append(':').append(value.toString()).append('\n');
			}
		}
	}
}
