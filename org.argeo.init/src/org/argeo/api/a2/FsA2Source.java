package org.argeo.api.a2;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.osgi.framework.Version;

/** A file system {@link AbstractProvisioningSource} in A2 format. */
public class FsA2Source extends AbstractProvisioningSource implements A2Source {
	private final static Logger logger = System.getLogger(FsA2Source.class.getName());

	private final Path base;
	private final Map<String, String> variantsXOr;

	private final List<String> includes;
	private final List<String> excludes;

	public FsA2Source(Path base, Map<String, String> variantsXOr, boolean usingReference, List<String> includes,
			List<String> excludes) {
		super(usingReference);
		this.base = base;
		this.variantsXOr = new HashMap<>(variantsXOr);
		this.includes = includes;
		this.excludes = excludes;
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

					// a variant was found, let's collect its contributions (also common ones in its
					// parent)
					if (Files.exists(variantPath.getParent())) {
						for (Path variantContributionPath : Files.newDirectoryStream(variantPath.getParent())) {
							String variantContributionId = variantContributionPath.getFileName().toString();
							if (variantContributionId.contains(".")) {
								A2Contribution contribution = getOrAddContribution(variantContributionId);
								contributions.put(variantContributionPath, contribution);
							}
						}
					}
					if (Files.exists(variantPath)) {
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

		contributions: for (Path contributionPath : contributions.keySet()) {
			String contributionId = contributionPath.getFileName().toString();
			if (includes != null && !includes.contains(contributionId))
				continue contributions;
			if (excludes != null && excludes.contains(contributionId))
				continue contributions;
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
						logger.log(Level.TRACE, () -> "Ignore " + modulePath + " since version cannot be found");
						continue modules;
					}
//					}
					A2Component component = contribution.getOrAddComponent(componentName);
					A2Module module = component.getOrAddModule(version, modulePath);
					logger.log(Level.TRACE, () -> "Registered " + module);
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
