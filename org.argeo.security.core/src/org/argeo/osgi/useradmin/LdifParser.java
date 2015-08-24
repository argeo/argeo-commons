package org.argeo.osgi.useradmin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Basic LDIF parser. */
class LdifParser {
	private final static Log log = LogFactory.getLog(LdifParser.class);

	SortedMap<LdapName, Attributes> read(InputStream in) throws IOException {
		SortedMap<LdapName, Attributes> res = new TreeMap<LdapName, Attributes>();
		try {
			List<String> lines = IOUtils.readLines(in);

			LdapName currentDn = null;
			Attributes currentAttributes = null;
			StringBuilder currentEntry = new StringBuilder();

			readLines: for (String line : lines) {
				if (line.startsWith(" ")) {
					currentEntry.append(line.substring(1));
					continue readLines;
				} else {
					if (currentEntry.length() != 0) {
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
						if (attributeId.equals("dn")) {
							if (currentDn != null) {
								Attributes previous = res.put(currentDn,
										currentAttributes);
								if (previous != null) {
									log.warn("There was already an entry with DN "
											+ currentDn
											+ ", which has been discarded by a subsequent one.");
								}
							}

							try {
								currentDn = new LdapName(
										attributeValue.toString());
								currentAttributes = new BasicAttributes();
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
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
		return res;
	}
}