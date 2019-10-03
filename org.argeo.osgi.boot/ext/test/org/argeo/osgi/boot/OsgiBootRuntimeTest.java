/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.osgi.boot;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Starts an Equinox runtime and provision it with OSGi boot. */
public class OsgiBootRuntimeTest extends TestCase {
	protected OsgiBoot osgiBoot = null;
	private boolean osgiRuntimeAlreadyRunning = false;

	public void testInstallAndStart() throws Exception {
		if (osgiRuntimeAlreadyRunning) {
			System.out
					.println("OSGi runtime already running, skipping test...");
			return;
		}
		osgiBoot.installUrls(osgiBoot.getBundlesUrls(OsgiBoot.DEFAULT_BASE_URL,
				OsgiBootNoRuntimeTest.BUNDLES));
		Map<String, Bundle> map = new TreeMap<String, Bundle>(
				osgiBoot.getBundlesBySymbolicName());
		for (Iterator<String> keys = map.keySet().iterator(); keys.hasNext();) {
			String key = keys.next();
			Bundle bundle = map.get(key);
			System.out.println(key + " : " + bundle.getLocation());
		}
		assertEquals(4, map.size());
		Iterator<String> keys = map.keySet().iterator();
		assertEquals("org.argeo.osgi.boot.test.bundle1", keys.next());
		assertEquals("org.argeo.osgi.boot.test.bundle2", keys.next());
		assertEquals("org.argeo.osgi.boot.test.bundle3", keys.next());
		assertEquals("org.eclipse.osgi", keys.next());

		// osgiBoot.startBundles("org.argeo.osgi.boot.test.bundle2");
		long begin = System.currentTimeMillis();
		while (System.currentTimeMillis() - begin < 10000) {
			Map<String, Bundle> mapBundles = osgiBoot
					.getBundlesBySymbolicName();
			Bundle bundle = mapBundles.get("org.argeo.osgi.boot.test.bundle2");
			if (bundle.getState() == Bundle.ACTIVE) {
				System.out.println("Bundle " + bundle + " started.");
				return;
			}
		}
		fail("Bundle not started after timeout limit.");
	}

	protected BundleContext startRuntime() throws Exception {
		String[] args = { "-console", "-clean" };
		BundleContext bundleContext = EclipseStarter.startup(args, null);

//		ServiceLoader<FrameworkFactory> ff = ServiceLoader.load(FrameworkFactory.class);
//		Map<String,String> config = new HashMap<String,String>();		
//		Framework fwk = ff.iterator().next().newFramework(config);
//		fwk.start();
		return bundleContext;
	}

	protected void stopRuntime() throws Exception {
		EclipseStarter.shutdown();
	}

	public void setUp() throws Exception {
		osgiRuntimeAlreadyRunning = EclipseStarter.isRunning();
		if (osgiRuntimeAlreadyRunning)
			return;
		BundleContext bundleContext = startRuntime();
		osgiBoot = new OsgiBoot(bundleContext);
	}

	public void tearDown() throws Exception {
		if (osgiRuntimeAlreadyRunning)
			return;
		osgiBoot = null;
		stopRuntime();
	}

}
