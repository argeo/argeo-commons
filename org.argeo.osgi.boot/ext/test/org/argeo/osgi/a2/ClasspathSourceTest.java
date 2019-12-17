package org.argeo.osgi.a2;

import java.io.IOException;

import org.argeo.osgi.a2.ClasspathSource;
import org.argeo.osgi.a2.ProvisioningManager;
import org.argeo.osgi.boot.equinox.EquinoxUtils;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class ClasspathSourceTest {
	@Test
	public void testProvisioning() throws IOException {
		Framework framework = EquinoxUtils.launch(null);
		ProvisioningManager provisioningManager = new ProvisioningManager(framework.getBundleContext());
		ClasspathSource classpathSource = new ClasspathSource();
		classpathSource.load();
		provisioningManager.addSource(classpathSource);
		provisioningManager.install(null);
		for (Bundle bundle : framework.getBundleContext().getBundles()) {
			System.out.println(bundle.getSymbolicName() + ":" + bundle.getVersion());
		}
		try {
			framework.stop();
		} catch (BundleException e) {
			// silent
		}
	}
}
