package org.argeo.api.a2;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.osgi.framework.Version;

/**
 * A provisioning source based on the linear classpath with which the JVM has
 * been started.
 */
public class ClasspathSource extends AbstractProvisioningSource {
	private final static Logger logger = System.getLogger(ClasspathSource.class.getName());

	public ClasspathSource() {
		super(true);
	}

	void load() throws IOException {
		A2Contribution classpathContribution = getOrAddContribution(A2Contribution.CLASSPATH);
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
			logger.log(Level.TRACE, () -> "Registered " + module);
		}

	}
}
