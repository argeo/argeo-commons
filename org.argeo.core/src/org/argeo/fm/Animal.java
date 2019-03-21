package org.argeo.fm;

public class Animal {
	String size;
	int price;

	public Animal(String size, int price) {
		super();
		this.size = size;
		this.price = price;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

}