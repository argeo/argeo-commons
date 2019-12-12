package org.argeo.osgi.boot.a2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.argeo.osgi.boot.OsgiBootUtils;
import org.osgi.framework.Version;

/**
 * A provisioning source based on the linear classpath with which the JCM has
 * been started.
 */
public class ClasspathSource extends ProvisioningSource {
	void load() throws IOException {
		A2Contribution classpathContribution = new A2Contribution(this, A2Contribution.CLASSPATH);
		List<String> classpath = Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator));
		parts: for (String part : classpath) {
			Path file = Paths.get(part);
			Version version;
			try {
				version = new Version(readVersionFromModule(file));
			} catch (Exception e) {
				// ignore non OSGi
				continue parts;
			}
			String moduleName = readSymbolicNameFromModule(file);
			A2Component component = classpathContribution.getOrAddComponent(moduleName);
			A2Module module = component.getOrAddModule(version, file);
			if (OsgiBootUtils.isDebug())
				OsgiBootUtils.debug("Registered " + module);
		}

	}
}
