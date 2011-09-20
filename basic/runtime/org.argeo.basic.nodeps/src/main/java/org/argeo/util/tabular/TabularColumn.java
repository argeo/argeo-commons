package org.argeo.util.tabular;

/** The column in a tabular content */
public class TabularColumn {
	private String name;
	/**
	 * JCR types, see
	 * http://www.day.com/maven/javax.jcr/javadocs/jcr-2.0/index.html
	 * ?javax/jcr/Property.html
	 */
	private Integer type;

	public TabularColumn(String name, Integer type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

}
