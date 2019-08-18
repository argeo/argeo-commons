package org.argeo.osgi.provisioning;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

import org.osgi.service.provisioning.ProvisioningService;

public class SimpleProvisioningService implements ProvisioningService {
	private Map<String, Object> map = Collections.synchronizedSortedMap(new TreeMap<>());

	public SimpleProvisioningService() {
		// update count
		map.put(PROVISIONING_UPDATE_COUNT, 0);
	}

	@Override
	public Dictionary<String, Object> getInformation() {
		return new Information();
	}

	@Override
	public synchronized void setInformation(Dictionary<String, ?> info) {
		map.clear();
		addInformation(info);
	}

	@Override
	public synchronized void addInformation(Dictionary<String, ?> info) {
		Enumeration<String> e = info.keys();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			map.put(key, info.get(key));
		}
		incrementProvisioningUpdateCount();
	}

	protected synchronized void incrementProvisioningUpdateCount() {
		Integer current = (Integer) map.get(PROVISIONING_UPDATE_COUNT);
		Integer newValue = current + 1;
		map.put(PROVISIONING_UPDATE_COUNT, newValue);
	}

	@Override
	public synchronized void addInformation(ZipInputStream zis) throws IOException {
		throw new UnsupportedOperationException();
	}

	class Information extends Dictionary<String, Object> {

		@Override
		public int size() {
			return map.size();
		}

		@Override
		public boolean isEmpty() {
			return map.isEmpty();
		}

		@Override
		public Enumeration<String> keys() {
			Iterator<String> it = map.keySet().iterator();
			return new Enumeration<String>() {

				@Override
				public boolean hasMoreElements() {
					return it.hasNext();
				}

				@Override
				public String nextElement() {
					return it.next();
				}

			};
		}

		@Override
		public Enumeration<Object> elements() {
			Iterator<Object> it = map.values().iterator();
			return new Enumeration<Object>() {

				@Override
				public boolean hasMoreElements() {
					return it.hasNext();
				}

				@Override
				public Object nextElement() {
					return it.next();
				}

			};
		}

		@Override
		public Object get(Object key) {
			return map.get(key);
		}

		@Override
		public Object put(String key, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

	}
}
