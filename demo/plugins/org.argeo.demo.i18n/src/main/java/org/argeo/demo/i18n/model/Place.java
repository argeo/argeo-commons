package org.argeo.demo.i18n.model;

import org.argeo.eclipse.ui.TreeParent;

public class Place extends TreeParent {

	private String description;
	private String address;

	public Place(String name, String description, String address) {
		super(name);
		this.description = description;
		this.address = address;
	}

	public void setAddress(String adress) {
		this.address = adress;
	}

	public String getAdress() {
		return address;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
