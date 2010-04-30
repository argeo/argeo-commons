package org.argeo.jcr;

import java.util.UUID;

public class SimpleObject {
	private String string;
	private String uuid = UUID.randomUUID().toString();
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

	@Override
	public boolean equals(Object obj) {
		return string.equals(((SimpleObject) obj).string);
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

}
