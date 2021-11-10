package org.argeo.naming;

class SrvRecord implements Comparable<SrvRecord> {
	private final Integer priority;
	private final Integer weight;
	private final Integer port;
	private final String hostname;

	public SrvRecord(Integer priority, Integer weight, Integer port, String hostname) {
		this.priority = priority;
		this.weight = weight;
		this.port = port;
		this.hostname = hostname;
	}

	@Override
	public int compareTo(SrvRecord other) {
		// https: // en.wikipedia.org/wiki/SRV_record
		if (priority != other.priority)
			return priority - other.priority;
		if (weight != other.weight)
			return other.weight - other.weight;
		String host = toHost(false);
		String otherHost = other.toHost(false);
		if (host.length() == otherHost.length())
			return host.compareTo(otherHost);
		else
			return host.length() - otherHost.length();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SrvRecord) {
			SrvRecord other = (SrvRecord) obj;
			return priority == other.priority && weight == other.weight && port == other.port
					&& hostname.equals(other.hostname);
		}
		return false;
	}

	@Override
	public String toString() {
		return priority + " " + weight;
	}

	public String toHost(boolean withPort) {
		String hostStr = hostname;
		if (hostname.charAt(hostname.length() - 1) == '.')
			hostStr = hostname.substring(0, hostname.length() - 1);
		return hostStr + (withPort ? ":" + port : "");
	}
}
