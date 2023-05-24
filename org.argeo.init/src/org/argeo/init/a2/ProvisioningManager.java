package org.argeo.init.a2;

import static org.argeo.init.a2.A2Source.SCHEME_A2;
import static org.argeo.init.a2.A2Source.SCHEME_A2_REFERENCE;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argeo.init.osgi.OsgiBootUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;

/** Loads provisioning sources into an OSGi context. */
public class ProvisioningManager {
	BundleContext bc;
	OsgiContext osgiContext;
	List<ProvisioningSource> sources = Collections.synchronizedList(new ArrayList<>());

	public ProvisioningManager(BundleContext bc) {
		this.bc = bc;
		osgiContext = new OsgiContext(bc);
		osgiContext.load();
	}

	protected void addSource(ProvisioningSource source) {
		sources.add(source);
	}

	void installWholeSource(ProvisioningSource source) {
		Set<Bundle> updatedBundles = new HashSet<>();
		for (A2Contribution contribution : source.listContributions(null)) {
			for (A2Component component : contribution.components.values()) {
				A2Module module = component.last().last();
				Bundle bundle = installOrUpdate(module);
				if (bundle != null)
					updatedBundles.add(bundle);
			}
		}
//		FrameworkWiring frameworkWiring = bc.getBundle(0).adapt(FrameworkWiring.class);
//		frameworkWiring.refreshBundles(updatedBundles);
	}

	public void registerSource(String uri) {
		try {
			URI u = new URI(uri);

			// XOR
			Map<String, List<String>> properties = queryToMap(u);
			Map<String, String> xOr = new HashMap<>();
			for (String key : properties.keySet()) {
				List<String> lst = properties.get(key);
				if (lst.size() != 1)
					throw new IllegalArgumentException("Invalid XOR definitions in " + uri);
				xOr.put(key, lst.get(0));
			}

			if (SCHEME_A2.equals(u.getScheme()) || SCHEME_A2_REFERENCE.equals(u.getScheme())) {
				if (u.getHost() == null || "".equals(u.getHost())) {
					String baseStr = u.getPath();
					if (File.separatorChar == '\\') {// MS Windows
						baseStr = baseStr.substring(1).replace('/', File.separatorChar);
					}
					Path base = Paths.get(baseStr);
					if (Files.exists(base)) {
						FsA2Source source = new FsA2Source(base, xOr, SCHEME_A2_REFERENCE.equals(u.getScheme()));
						source.load();
						addSource(source);
						OsgiBootUtils.info("Registered " + uri + " as source");

						// OS specific / native
						String localRelPath = A2Contribution.localOsArchRelativePath();
						Path localLibBase = base.resolve(A2Contribution.LIB).resolve(localRelPath);
						if (Files.exists(localLibBase)) {
							FsA2Source libSource = new FsA2Source(localLibBase, xOr,
									SCHEME_A2_REFERENCE.equals(u.getScheme()));
							libSource.load();
							addSource(libSource);
							OsgiBootUtils.info("Registered OS-specific " + uri + " as source (" + localRelPath + ")");
						}
					} else {
						OsgiBootUtils.debug("Source " + base + " does not exist, ignoring.");
					}
				} else {
					throw new UnsupportedOperationException(
							"Remote installation is not yet supported, cannot add source " + u);
				}
			} else {
				throw new IllegalArgumentException("Unkown scheme: for source " + u);
			}
		} catch (Exception e) {
			throw new A2Exception("Cannot add source " + uri, e);
		}
	}

	public boolean registerDefaultSource() {
		String frameworkLocation = bc.getProperty("osgi.framework");
		try {
			URI frameworkLocationUri = new URI(frameworkLocation);
			if ("file".equals(frameworkLocationUri.getScheme())) {
				Path frameworkPath = Paths.get(frameworkLocationUri);
				if (frameworkPath.getParent().getFileName().toString().equals(A2Contribution.BOOT)) {
					Path base = frameworkPath.getParent().getParent();
					String baseStr = base.toString();
					if (File.separatorChar == '\\')// MS Windows
						baseStr = '/' + baseStr.replace(File.separatorChar, '/');
					URI baseUri = new URI(A2Source.SCHEME_A2, null, null, 0, baseStr, null, null);
					registerSource(baseUri.toString());
					OsgiBootUtils.debug("Default source from framework location " + frameworkLocation);
					return true;
				}
			}
		} catch (Exception e) {
			OsgiBootUtils.error("Cannot register default source based on framework location " + frameworkLocation, e);
		}
		return false;
	}

	public void install(String spec) {
		if (spec == null) {
			for (ProvisioningSource source : sources) {
				installWholeSource(source);
			}
		}
	}

	/** @return the new/updated bundle, or null if nothing was done. */
	protected Bundle installOrUpdate(A2Module module) {
		try {
			ProvisioningSource moduleSource = module.getBranch().getComponent().getContribution().getSource();
			Version moduleVersion = module.getVersion();
			A2Branch osgiBranch = osgiContext.findBranch(module.getBranch().getComponent().getId(), moduleVersion);
			if (osgiBranch == null) {
//				Bundle bundle = bc.installBundle(module.getBranch().getCoordinates(),
//						moduleSource.newInputStream(module.getLocator()));
				Bundle bundle = moduleSource.install(bc, module);
				if (OsgiBootUtils.isDebug())
					OsgiBootUtils.debug("Installed bundle " + bundle.getLocation() + " with version " + moduleVersion);
				return bundle;
			} else {
				A2Module lastOsgiModule = osgiBranch.last();
				int compare = moduleVersion.compareTo(lastOsgiModule.getVersion());
				if (compare > 0) {// update
					Bundle bundle = (Bundle) lastOsgiModule.getLocator();
//					bundle.update(moduleSource.newInputStream(module.getLocator()));
					moduleSource.update(bundle, module);
					OsgiBootUtils.info("Updated bundle " + bundle.getLocation() + " to version " + moduleVersion);
					return bundle;
				}
			}
		} catch (Exception e) {
			OsgiBootUtils.error("Could not install module " + module + ": " + e.getMessage(), e);
		}
		return null;
	}

	public Collection<Bundle> update() {
		boolean fragmentsUpdated = false;
		Set<Bundle> updatedBundles = new HashSet<>();
		bundles: for (Bundle bundle : bc.getBundles()) {
			for (ProvisioningSource source : sources) {
				String componentId = bundle.getSymbolicName();
				Version version = bundle.getVersion();
				A2Branch branch = source.findBranch(componentId, version);
				if (branch == null)
					continue bundles;
				A2Module module = branch.last();
				Version moduleVersion = module.getVersion();
				int compare = moduleVersion.compareTo(version);
				if (compare > 0) {// update
					try {
						source.update(bundle, module);
//						bundle.update(in);
						String fragmentHost = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
						if (fragmentHost != null)
							fragmentsUpdated = true;
						OsgiBootUtils.info("Updated bundle " + bundle.getLocation() + " to version " + moduleVersion);
						updatedBundles.add(bundle);
					} catch (Exception e) {
						OsgiBootUtils.error("Cannot update with module " + module, e);
					}
				}
			}
		}
		FrameworkWiring frameworkWiring = bc.getBundle(0).adapt(FrameworkWiring.class);
		if (fragmentsUpdated)// refresh all
			frameworkWiring.refreshBundles(null);
		else
			frameworkWiring.refreshBundles(updatedBundles);
		return updatedBundles;
	}

	private static Map<String, List<String>> queryToMap(URI uri) {
		return queryToMap(uri.getQuery());
	}

	private static Map<String, List<String>> queryToMap(String queryPart) {
		try {
			final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
			if (queryPart == null)
				return query_pairs;
			final String[] pairs = queryPart.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name())
						: pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1
						? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name())
						: null;
				query_pairs.get(key).add(value);
			}
			return query_pairs;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Cannot convert " + queryPart + " to map", e);
		}
	}

//	public static void main(String[] args) {
//		if (args.length == 0)
//			throw new IllegalArgumentException("Usage: <path to A2 base>");
//		Map<String, String> configuration = new HashMap<>();
//		configuration.put("osgi.console", "2323");
//		configuration.put("org.osgi.framework.bootdelegation",
//				"com.sun.jndi.ldap,com.sun.jndi.ldap.sasl,com.sun.security.jgss,com.sun.jndi.dns,com.sun.nio.file,com.sun.nio.sctp,sun.nio.cs");
//		Framework framework = OsgiBootUtils.launch(configuration);
//		try {
//			ProvisioningManager pm = new ProvisioningManager(framework.getBundleContext());
//			Map<String, String> xOr = new HashMap<>();
//			xOr.put("osgi", "equinox");
//			xOr.put("swt", "rap");
//			FsA2Source context = new FsA2Source(Paths.get(args[0]), xOr);
//			context.load();
//			pm.addSource(context);
//			if (framework.getBundleContext().getBundles().length == 1) {// initial
//				pm.install(null);
//			} else {
//				pm.update();
//			}
//
//			Thread.sleep(2000);
//
//			Bundle[] bundles = framework.getBundleContext().getBundles();
//			Arrays.sort(bundles, (b1, b2) -> b1.getSymbolicName().compareTo(b2.getSymbolicName()));
//			for (Bundle b : bundles)
//				if (b.getState() == Bundle.RESOLVED || b.getState() == Bundle.STARTING || b.getState() == Bundle.ACTIVE)
//					System.out.println(b.getSymbolicName() + " " + b.getVersion());
//				else
//					System.err.println(b.getSymbolicName() + " " + b.getVersion() + " (" + b.getState() + ")");
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				framework.stop();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

}
