package org.argeo.osgi.useradmin;

import static org.argeo.osgi.useradmin.LdifName.dn;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.codec.binary.Base64;

/** Basic LDIF writer */
class LdifWriter {
	private final Writer writer;

	LdifWriter(OutputStream out) {
		this.writer = new OutputStreamWriter(out);
	}

	void writeEntry(LdapName name, Attributes attributes) throws IOException {
		try {
			// check consistency
			Rdn nameRdn = name.getRdn(name.size() - 1);
			Attribute nameAttr = attributes.get(nameRdn.getType());
			if (!nameAttr.get().equals(nameRdn.getValue()))
				throw new UserDirectoryException("Attribute "
						+ nameAttr.getID() + "=" + nameAttr.get()
						+ " not consistent with DN " + name);

			writer.append(dn.name() + ":").append(name.toString()).append('\n');
			Attribute objectClassAttr = attributes.get("objectClass");
			if (objectClassAttr != null)
				writeAttribute(objectClassAttr);
			for (NamingEnumeration<? extends Attribute> attrs = attributes
					.getAll(); attrs.hasMore();) {
				Attribute attribute = attrs.next();
				if (attribute.getID().equals(dn.name())
						|| attribute.getID().equals("objectClass"))
					continue;// skip DN attribute
				writeAttribute(attribute);
			}
			writer.append('\n');
			writer.flush();
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot write LDIF", e);
		}
	}

	private void writeAttribute(Attribute attribute) throws NamingException,
			IOException {
		for (NamingEnumeration<?> attrValues = attribute.getAll(); attrValues
				.hasMore();) {
			Object value = attrValues.next();
			if (value instanceof byte[]) {
				String encoded = Base64.encodeBase64String((byte[]) value);
				writer.append(attribute.getID()).append("::").append(encoded)
						.append('\n');
			} else {
				writer.append(attribute.getID()).append(':')
						.append(value.toString()).append('\n');
			}
		}
	}
}
