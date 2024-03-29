package org.argeo.cms.dns;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class DnsBrowser implements Closeable {
	private final DirContext initialCtx;

	public DnsBrowser() {
		this(new ArrayList<>());
	}

	public DnsBrowser(List<String> dnsServerUrls) {
		try {
			Objects.requireNonNull(dnsServerUrls);
			Hashtable<String, Object> env = new Hashtable<>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
			if (!dnsServerUrls.isEmpty()) {
				boolean specified = false;
				StringJoiner providerUrl = new StringJoiner(" ");
				for (String dnsUrl : dnsServerUrls) {
					if (dnsUrl != null) {
						providerUrl.add(dnsUrl);
						specified = true;
					}
				}
				if (specified)
					env.put(Context.PROVIDER_URL, providerUrl.toString());
			}
			initialCtx = new InitialDirContext(env);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot initialise DNS borowser.", e);
		}
	}

	public Map<String, List<String>> getAllRecords(String name) {
		try {
			Map<String, List<String>> res = new TreeMap<>();
			Attributes attrs = initialCtx.getAttributes(name);
			NamingEnumeration<String> ids = attrs.getIDs();
			while (ids.hasMore()) {
				String recordType = ids.next();
				List<String> lst = new ArrayList<String>();
				res.put(recordType, lst);
				Attribute attr = attrs.get(recordType);
				addValues(attr, lst);
			}
			return Collections.unmodifiableMap(res);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get allrecords of " + name, e);
		}
	}

	/**
	 * Return a single record (typically A, AAAA, etc. or null if not available.
	 * Will fail if multiple records.
	 */
	public String getRecord(String name, String recordType) {
		try {
			Attributes attrs = initialCtx.getAttributes(name, new String[] { recordType });
			if (attrs.size() == 0)
				return null;
			Attribute attr = attrs.get(recordType);
			if (attr.size() > 1)
				throw new IllegalArgumentException("Multiple record type " + recordType);
			assert attr.size() != 0;
			Object value = attr.get();
			assert value != null;
			return value.toString();
		} catch (NameNotFoundException e) {
			return null;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get DNS entry " + recordType + " of " + name, e);
		}
	}

	/**
	 * Return records of a given type.
	 */
	public List<String> getRecords(String name, String recordType) {
		try {
			List<String> res = new ArrayList<String>();
			Attributes attrs = initialCtx.getAttributes(name, new String[] { recordType });
			Attribute attr = attrs.get(recordType);
			addValues(attr, res);
			return res;
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot get records " + recordType + " of " + name, e);
		}
	}

	/** Ordered, with preferred first. */
	public List<String> getSrvRecordsAsHosts(String name, boolean withPort) {
		List<String> raw = getRecords(name, "SRV");
		if (raw.size() == 0)
			return null;
		SortedSet<SrvRecord> res = new TreeSet<>();
		for (int i = 0; i < raw.size(); i++) {
			String record = raw.get(i);
			String[] arr = record.split(" ");
			Integer priority = Integer.parseInt(arr[0]);
			Integer weight = Integer.parseInt(arr[1]);
			Integer port = Integer.parseInt(arr[2]);
			String hostname = arr[3];
			SrvRecord order = new SrvRecord(priority, weight, port, hostname);
			res.add(order);
		}
		List<String> lst = new ArrayList<>();
		for (SrvRecord order : res) {
			lst.add(order.toHost(withPort));
		}
		return Collections.unmodifiableList(lst);
	}

	private void addValues(Attribute attr, List<String> lst) throws NamingException {
		NamingEnumeration<?> values = attr.getAll();
		while (values.hasMore()) {
			Object value = values.next();
			if (value != null) {
				if (value instanceof byte[]) {
					String str = Base64.getEncoder().encodeToString((byte[]) value);
					lst.add(str);
				} else
					lst.add(value.toString());
			}
		}

	}

	public List<String> listEntries(String name) {
		try {
			List<String> res = new ArrayList<String>();
			NamingEnumeration<Binding> ne = initialCtx.listBindings(name);
			while (ne.hasMore()) {
				Binding b = ne.next();
				res.add(b.getName());
			}
			return Collections.unmodifiableList(res);
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot list entries of " + name, e);
		}
	}

	@Override
	public void close() throws IOException {
		destroy();
	}

	public void destroy() {
		try {
			initialCtx.close();
		} catch (NamingException e) {
			// silent
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			printUsage(System.err);
			System.exit(1);
		}
		try (DnsBrowser dnsBrowser = new DnsBrowser()) {
			String hostname = args[0];
			String recordType = args.length > 1 ? args[1] : "A";
			if (recordType.equals("*")) {
				Map<String, List<String>> records = dnsBrowser.getAllRecords(hostname);
				for (String type : records.keySet()) {
					for (String record : records.get(type)) {
						String typeLabel;
						if ("44".equals(type))
							typeLabel = "SSHFP";
						else if ("46".equals(type))
							typeLabel = "RRSIG";
						else if ("48".equals(type))
							typeLabel = "DNSKEY";
						else
							typeLabel = type;
						System.out.println(typeLabel + "\t" + record);
					}
				}
			} else {
				System.out.println(dnsBrowser.getRecord(hostname, recordType));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void printUsage(PrintStream out) {
		out.println("java org.argeo.naming.DnsBrowser <hostname> [<record type> | *]");
	}

}