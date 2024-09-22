package org.argeo.cms.servlet.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.argeo.cms.osgi.FilterRequirement;
import org.argeo.cms.osgi.PublishNamespace;
import org.argeo.cms.util.StreamUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;

/**
 * Publishes client-side web resources (JavaScript, HTML, CSS, images, etc.)
 * from the OSGi runtime.
 */
public class PkgServlet extends HttpServlet {
	private static final long serialVersionUID = 7660824185145214324L;

	private static FileNameMap fileNameMap = URLConnection.getFileNameMap();

	private BundleContext bundleContext = FrameworkUtil.getBundle(PkgServlet.class).getBundleContext();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();

		String pkg, versionStr, file;
		String[] parts = pathInfo.split("/");
		// first is always empty
		if (parts.length == 4) {
			pkg = parts[1];
			versionStr = parts[2];
			file = parts[3];
		} else if (parts.length == 3) {
			pkg = parts[1];
			versionStr = null;
			file = parts[2];
		} else {
			throw new IllegalArgumentException("Unsupported path length " + pathInfo);
		}

		// content type
		String contentType = fileNameMap.getContentTypeFor(file);
		resp.setContentType(contentType);

		FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
		String filter;
		if (versionStr == null) {
			filter = "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + pkg + ")";
		} else {
			if (versionStr.startsWith("[") || versionStr.startsWith("(")) {// range
				VersionRange versionRange = new VersionRange(versionStr);
				filter = "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + pkg + ")"
						+ versionRange.toFilterString(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE) + ")";

			} else {
				Version version = new Version(versionStr);
				filter = "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + pkg + ")("
						+ PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=" + version + "))";
			}
		}
		Requirement requirement = new FilterRequirement(PackageNamespace.PACKAGE_NAMESPACE, filter);
		Collection<BundleCapability> packages = frameworkWiring.findProviders(requirement);
		if (packages.isEmpty()) {
			resp.sendError(404);
			return;
		}

		// TODO verify that it works with multiple versions
		SortedMap<Version, BundleCapability> sorted = new TreeMap<>();
		for (BundleCapability capability : packages) {
			sorted.put(capability.getRevision().getVersion(), capability);
		}

		Bundle bundle = sorted.get(sorted.firstKey()).getRevision().getBundle();
		String entryPath = '/' + pkg.replace('.', '/') + '/' + file;
		URL internalURL = bundle.getResource(entryPath);
		if (internalURL == null) {
			resp.sendError(404);
			return;
		}

		// Resource found, we now check whether it can be published
		boolean publish = false;
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		capabilities: for (BundleCapability bundleCapability : bundleWiring
				.getCapabilities(PublishNamespace.CMS_PUBLISH_NAMESPACE)) {
			Object publishedPkg = bundleCapability.getAttributes().get(PublishNamespace.PKG);
			if (publishedPkg != null) {
				if (publishedPkg.equals("*") || publishedPkg.equals(pkg)) {
					Object publishedFile = bundleCapability.getAttributes().get(PublishNamespace.FILE);
					if (publishedFile == null) {
						publish = true;
						break capabilities;
					} else {
						String[] publishedFiles = publishedFile.toString().split(",");
						for (String pattern : publishedFiles) {
							if (pattern.startsWith("*.")) {
								String ext = pattern.substring(1);
								if (file.endsWith(ext)) {
									publish = true;
									break capabilities;
								}
							} else {
								if (publishedFile.equals(file)) {
									publish = true;
									break capabilities;
								}
							}
						}
					}
				}
			}
		}

		if (!publish) {
			resp.sendError(404);
			return;
		}

		try (InputStream in = internalURL.openStream()) {
			StreamUtils.copy(in, resp.getOutputStream());
		}
	}

}
