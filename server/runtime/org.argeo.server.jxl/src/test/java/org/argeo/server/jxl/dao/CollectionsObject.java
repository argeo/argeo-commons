package org.argeo.server.jxl.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionsObject {
	private String id;
	private String label;
	private SimpleObject simpleObject;
	private List<String> stringList = new ArrayList<String>();
	private Map<String, Float> floatMap = new HashMap<String, Float>();
	private Map<SimpleObject, String> objectMap = new HashMap<SimpleObject, String>();
	private Map<String, Map<String, String>> mapOfMaps = new HashMap<String, Map<String, String>>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public SimpleObject getSimpleObject() {
		return simpleObject;
	}

	public void setSimpleObject(SimpleObject simpleObject) {
		this.simpleObject = simpleObject;
	}

	public List<String> getStringList() {
		return stringList;
	}

	public void setStringList(List<String> stringList) {
		this.stringList = stringList;
	}

	public Map<String, Float> getFloatMap() {
		return floatMap;
	}

	public void setFloatMap(Map<String, Float> floatMap) {
		this.floatMap = floatMap;
	}

	public Map<SimpleObject, String> getObjectMap() {
		return objectMap;
	}

	public void setObjectMap(Map<SimpleObject, String> objectMap) {
		this.objectMap = objectMap;
	}

	public Map<String, Map<String, String>> getMapOfMaps() {
		return mapOfMaps;
	}

	public void setMapOfMaps(Map<String, Map<String, String>> mapOfMaps) {
		this.mapOfMaps = mapOfMaps;
	}
}
