package org.argeo.server.jxl.dao;

public class SimpleObject {
	private String string;
	private Integer integer;
	private OtherObject otherObject;
	private OtherObject anotherObject;

	public String getString() {
		return string;
	}

	public void setString(String sting) {
		this.string = sting;
	}

	public Integer getInteger() {
		return integer;
	}

	public void setInteger(Integer integer) {
		this.integer = integer;
	}

	public OtherObject getOtherObject() {
		return otherObject;
	}

	public void setOtherObject(OtherObject otherObject) {
		this.otherObject = otherObject;
	}

	public OtherObject getAnotherObject() {
		return anotherObject;
	}

	public void setAnotherObject(OtherObject anotherObject) {
		this.anotherObject = anotherObject;
	}
}
