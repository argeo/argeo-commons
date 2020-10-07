package org.argeo.api;

import java.util.Map;

/**
 * Key used to classify and filter available components (typically provided by
 * OSGi services).
 */
public class RankingKey implements Comparable<RankingKey> {
	public final static String SERVICE_PID = "service.pid";
	public final static String SERVICE_ID = "service.id";
	public final static String SERVICE_RANKING = "service.ranking";

	private String pid;
	private Integer ranking = 0;
	private Long id;
	private String dataType;
	private String dataPath;

	public RankingKey(String pid, Integer ranking, Long id, String dataType, String dataPath) {
		super();
		this.pid = pid;
		this.ranking = ranking;
		this.id = id;
		this.dataType = dataType;
		this.dataPath = dataPath;
	}

	public RankingKey(Map<String, Object> properties) {
		this.pid = properties.containsKey(SERVICE_PID) ? properties.get(SERVICE_PID).toString() : null;
		this.ranking = properties.containsKey(SERVICE_RANKING)
				? Integer.parseInt(properties.get(SERVICE_RANKING).toString())
				: 0;
		this.id = properties.containsKey(SERVICE_ID) ? (Long) properties.get(SERVICE_ID) : null;
	}

	@Override
	public int hashCode() {
		Integer result = 0;
		if (pid != null)
			result = +pid.hashCode();
		if (ranking != null)
			result = +ranking;
		return result;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new RankingKey(pid, ranking, id, dataType, dataPath);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		if (pid != null)
			sb.append(pid);
		if (ranking != null && ranking != 0)
			sb.append(' ').append(ranking);
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RankingKey))
			return false;
		RankingKey other = (RankingKey) obj;
		return equalsOrBothNull(pid, other.pid) && equalsOrBothNull(ranking, other.ranking)
				&& equalsOrBothNull(id, other.id) && equalsOrBothNull(dataType, other.dataType)
				&& equalsOrBothNull(dataPath, other.dataPath);
	}

	@Override
	public int compareTo(RankingKey o) {
		if (pid != null && o.pid != null) {
			if (pid.equals(o.pid)) {
				if (ranking.equals(o.ranking))
					if (id != null && o.id != null)
						return id.compareTo(o.id);
					else
						return 0;
				else
					return ranking.compareTo(o.ranking);
			} else {
				return pid.compareTo(o.pid);
			}

		} else {

		}
		return 0;
	}

	public String getPid() {
		return pid;
	}

	public Integer getRanking() {
		return ranking;
	}

	public Long getId() {
		return id;
	}

	public String getDataType() {
		return dataType;
	}

	public String getDataPath() {
		return dataPath;
	}

	public static RankingKey minPid(String pid) {
		return new RankingKey(pid, Integer.MIN_VALUE, null, null, null);
	}

	public static RankingKey maxPid(String pid) {
		return new RankingKey(pid, Integer.MAX_VALUE, null, null, null);
	}

	private static boolean equalsOrBothNull(Object o1, Object o2) {
		if (o1 == null && o2 == null)
			return true;
		if (o1 == null && o2 != null)
			return false;
		if (o1 != null && o2 == null)
			return false;
		return o2.equals(o1);
	}
}
