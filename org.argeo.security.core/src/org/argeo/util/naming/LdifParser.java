package org.argeo.util.naming;

import static org.argeo.osgi.useradmin.LdifName.dn;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.osgi.useradmin.UserDirectoryException;

/** Basic LDIF parser. */
public class LdifParser {
	private final static Log log = LogFactory.getLog(LdifParser.class);

	protected Attributes addAttributes(SortedMap<LdapName, Attributes> res,
			int lineNumber, LdapName currentDn, Attributes currentAttributes) {
		try {
			Rdn nameRdn = currentDn.getRdn(currentDn.size() - 1);
			Attribute nameAttr = currentAttributes.get(nameRdn.getType());
			if (nameAttr == null)
				currentAttributes.put(nameRdn.getType(), nameRdn.getValue());
			else if (!nameAttr.get().equals(nameRdn.getValue()))
				throw new UserDirectoryException("Attribute "
						+ nameAttr.getID() + "=" + nameAttr.get()
						+ " not consistent with DN " + currentDn
						+ " (shortly before line " + lineNumber
						+ " in LDIF file)");
			Attributes previous = res.put(currentDn, currentAttributes);
			if (log.isTraceEnabled())
				log.trace("Added " + currentDn);
			return previous;
		} catch (NamingException e) {
			throw new UserDirectoryException("Cannot add " + currentDn, e);
		}
	}

	public SortedMap<LdapName, Attributes> read(InputStream in) throws IOException {
		SortedMap<LdapName, Attributes> res = new TreeMap<LdapName, Attributes>();
		try {
			List<String> lines = IOUtils.readLines(in);
			// add an empty new line since the last line is not checked
			if (!lines.get(lines.size() - 1).equals(""))
				lines.add("");

			LdapName currentDn = null;
			Attributes currentAttributes = null;
			StringBuilder currentEntry = new StringBuilder();

			readLines: for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
				String line = lines.get(lineNumber);
				boolean isLastLine = false;
				if (lineNumber == lines.size() - 1)
					isLastLine = true;
				if (line.startsWith(" ")) {
					currentEntry.append(line.substring(1));
					if (!isLastLine)
						continue readLines;
				}

				if (currentEntry.length() != 0 || isLastLine) {
					// read previous attribute
					StringBuilder attrId = new StringBuilder(8);
					boolean isBase64 = false;
					readAttrId: for (int i = 0; i < currentEntry.length(); i++) {
						char c = currentEntry.charAt(i);
						if (c == ':') {
							if (i + 1 < currentEntry.length()
									&& currentEntry.charAt(i + 1) == ':')
								isBase64 = true;
							currentEntry.delete(0, i + (isBase64 ? 2 : 1));
							break readAttrId;
						} else {
							attrId.append(c);
						}
					}

					String attributeId = attrId.toString();
					String cleanValueStr = currentEntry.toString().trim();
					Object attributeValue = isBase64 ? Base64
							.decodeBase64(cleanValueStr) : cleanValueStr;

					// manage DN attributes
					if (attributeId.equals(dn.name()) || isLastLine) {
						if (currentDn != null) {
							//
							// ADD
							//
							Attributes previous = addAttributes(res,
									lineNumber, currentDn, currentAttributes);
							if (previous != null) {
								log.warn("There was already an entry with DN "
										+ currentDn
										+ ", which has been discarded by a subsequent one.");
							}
						}

						if (attributeId.equals(dn.name()))
							try {
								currentDn = new LdapName(
										attributeValue.toString());
								currentAttributes = new BasicAttributes(true);
							} catch (InvalidNameException e) {
								log.error(attributeValue
										+ " not a valid DN, skipping the entry.");
								currentDn = null;
								currentAttributes = null;
							}
					}

					// store attribute
					if (currentAttributes != null) {
						Attribute attribute = currentAttributes
								.get(attributeId);
						if (attribute == null) {
							attribute = new BasicAttribute(attributeId);
							currentAttributes.put(attribute);
						}
						attribute.add(attributeValue);
					}
					currentEntry = new StringBuilder();
				}
				currentEntry.append(line);
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
		return res;
	}
}