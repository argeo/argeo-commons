package org.argeo.init.a2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.argeo.init.osgi.OsgiBootUtils;
import org.osgi.framework.Version;

/** A file system {@link AbstractProvisioningSource} in A2 format. */
public class FsA2Source extends AbstractProvisioningSource implements A2Source {
	private final Path base;
	private final Map<String, String> variantsXOr;

//	public FsA2Source(Path base) {
//		this(base, new HashMap<>());
//	}

	public FsA2Source(Path base, Map<String, String> variantsXOr, boolean usingReference) {
		super(usingReference);
		this.base = base;
		this.variantsXOr = new HashMap<>(variantsXOr);
	}

	void load() throws IOException {
		SortedMap<Path, A2Contribution> contributions = new TreeMap<>();

		DirectoryStream<Path> contributionPaths = Files.newDirectoryStream(base);
		contributions: for (Path contributionPath : contributionPaths) {
			if (Files.isDirectory(contributionPath)) {
				String contributionId = contributionPath.getFileName().toString();
				if (A2Contribution.BOOT.equals(contributionId))// skip boot
					continue contributions;
				if (contributionId.contains(".")) {
					A2Contribution contribution = getOrAddContribution(contributionId);
					contributions.put(contributionPath, contribution);
				} else {// variants
					Path variantPath = null;
					// is it an explicit variant?
					String variant = variantsXOr.get(contributionPath.getFileName().toString());
					if (variant != null) {
						variantPath = contributionPath.resolve(variant);
					}

					// is there a default variant?
					if (variantPath == null) {
						Path defaultPath = contributionPath.resolve(A2Contribution.DEFAULT);
						if (Files.exists(defaultPath)) {
							variantPath = defaultPath;
						}
					}

					if (variantPath == null)
						continue contributions;

					if (Files.exists(variantPath)) {
						// a variant was found, let's collect its contributions (also common ones in its
						// parent)
						for (Path variantContributionPath : Files.newDirectoryStream(variantPath.getParent())) {
							String variantContributionId = variantContributionPath.getFileName().toString();
							if (variantContributionId.contains(".")) {
								A2Contribution contribution = getOrAddContribution(variantContributionId);
								contributions.put(variantContributionPath, contribution);
							}
						}
						for (Path variantContributionPath : Files.newDirectoryStream(variantPath)) {
							String variantContributionId = variantContributionPath.getFileName().toString();
							if (variantContributionId.contains(".")) {
								A2Contribution contribution = getOrAddContribution(variantContributionId);
								contributions.put(variantContributionPath, contribution);
							}
						}
					}
				}
			}
		}

		for (Path contributionPath : contributions.keySet()) {
			String contributionId = contributionPath.getFileName().toString();
			A2Contribution contribution = getOrAddContribution(contributionId);
			DirectoryStream<Path> modulePaths = Files.newDirectoryStream(contributionPath);
			modules: for (Path modulePath : modulePaths) {
				if (!Files.isDirectory(modulePath)) {
					// OsgiBootUtils.debug("Registering " + modulePath);
					String moduleFileName = modulePath.getFileName().toString();
					int lastDot = moduleFileName.lastIndexOf('.');
					String ext = moduleFileName.substring(lastDot + 1);
					if (!"jar".equals(ext))
						continue modules;
					Version version;
					// TODO optimise? check attributes?
					String[] nameVersion = readNameVersionFromModule(modulePath);
					String componentName = nameVersion[0];
					String versionStr = nameVersion[1];
					if (versionStr != null) {
						version = new Version(versionStr);
					} else {
						OsgiBootUtils.debug("Ignore " + modulePath + " since version cannot be found");
						continue modules;
					}
//					}
					A2Component component = contribution.getOrAddComponent(componentName);
					A2Module module = component.getOrAddModule(version, modulePath);
					if (OsgiBootUtils.isDebug())
						OsgiBootUtils.debug("Registered " + module);
				}
			}
		}

	}

	@Override
	public URI getUri() {
		URI baseUri = base.toUri();
		try {
			if (baseUri.getScheme().equals("file")) {
				String queryPart = "";
				if (!getVariantsXOr().isEmpty()) {
					StringJoiner sj = new StringJoiner("&");
					for (String key : getVariantsXOr().keySet()) {
						sj.add(key + "=" + getVariantsXOr().get(key));
					}
					queryPart = sj.toString();
				}
				return new URI(isUsingReference() ? SCHEME_A2_REFERENCE : SCHEME_A2, null, base.toString(), queryPart,
						null);
			} else {
				throw new UnsupportedOperationException("Unsupported scheme " + baseUri.getScheme());
			}
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Cannot build URI from " + baseUri, e);
		}
	}

	protected Map<String, String> getVariantsXOr() {
		return variantsXOr;
	}

//	public static void main(String[] args) {
//		if (args.length == 0)
//			throw new IllegalArgumentException("Usage: <path to A2 base>");
//		try {
//			Map<String, String> xOr = new HashMap<>();
//			xOr.put("osgi", "equinox");
//			xOr.put("swt", "rap");
//			FsA2Source context = new FsA2Source(Paths.get(args[0]), xOr);
//			context.load();
//			context.asTree();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

}
