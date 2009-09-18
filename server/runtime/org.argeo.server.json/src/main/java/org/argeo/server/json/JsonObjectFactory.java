package org.argeo.server.json;


public interface JsonObjectFactory {
	public Boolean supports(String type);

	public <T> T readValue(String type, String str);
}
