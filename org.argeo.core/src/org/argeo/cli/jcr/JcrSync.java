package org.argeo.cli.jcr;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.cli.CommandArgsException;
import org.argeo.cli.CommandRuntimeException;
import org.argeo.cli.DescribedCommand;
import org.argeo.jackrabbit.client.ClientDavexRepositoryFactory;
import org.argeo.jcr.JcrUtils;
import org.argeo.sync.SyncResult;

public class JcrSync implements DescribedCommand<SyncResult<Node>> {
	public final static String DEFAULT_LOCALFS_CONFIG = "repository-localfs.xml";

	final static Option deleteOption = Option.builder().longOpt("delete").desc("delete from target").build();
	final static Option recursiveOption = Option.builder("r").longOpt("recursive").desc("recurse into directories")
			.build();
	final static Option progressOption = Option.builder().longOpt("progress").hasArg(false).desc("show progress")
			.build();

	@Override
	public SyncResult<Node> apply(List<String> t) {
		try {
			CommandLine line = toCommandLine(t);
			List<String> remaining = line.getArgList();
			if (remaining.size() == 0) {
				throw new CommandArgsException("There must be at least one argument");
			}
			URI sourceUri = new URI(remaining.get(0));
			URI targetUri;
			if (remaining.size() == 1) {
				targetUri = Paths.get(System.getProperty("user.dir")).toUri();
			} else {
				targetUri = new URI(remaining.get(1));
			}
			boolean delete = line.hasOption(deleteOption.getLongOpt());
			boolean recursive = line.hasOption(recursiveOption.getLongOpt());

			// TODO make it configurable
			String sourceWorkspace = "home";
			String targetWorkspace = sourceWorkspace;

			final Repository sourceRepository;
			final Session sourceSession;
			Credentials sourceCredentials = null;
			final Repository targetRepository;
			final Session targetSession;
			Credentials targetCredentials = null;

			if ("http".equals(sourceUri.getScheme()) || "https".equals(sourceUri.getScheme())) {
				sourceRepository = createRemoteRepository(sourceUri);
			} else if (null == sourceUri.getScheme() || "file".equals(sourceUri.getScheme())) {
				RepositoryConfig repositoryConfig = RepositoryConfig.create(
						JcrSync.class.getResourceAsStream(DEFAULT_LOCALFS_CONFIG), sourceUri.getPath().toString());
				sourceRepository = RepositoryImpl.create(repositoryConfig);
				sourceCredentials = new SimpleCredentials("admin", "admin".toCharArray());
			} else {
				throw new IllegalArgumentException("Unsupported scheme " + sourceUri.getScheme());
			}
			sourceSession = JcrUtils.loginOrCreateWorkspace(sourceRepository, sourceWorkspace, sourceCredentials);

			if ("http".equals(targetUri.getScheme()) || "https".equals(targetUri.getScheme())) {
				targetRepository = createRemoteRepository(targetUri);
			} else if (null == targetUri.getScheme() || "file".equals(targetUri.getScheme())) {
				RepositoryConfig repositoryConfig = RepositoryConfig.create(
						JcrSync.class.getResourceAsStream(DEFAULT_LOCALFS_CONFIG), targetUri.getPath().toString());
				targetRepository = RepositoryImpl.create(repositoryConfig);
				targetCredentials = new SimpleCredentials("admin", "admin".toCharArray());
			} else {
				throw new IllegalArgumentException("Unsupported scheme " + targetUri.getScheme());
			}
			targetSession = JcrUtils.loginOrCreateWorkspace(targetRepository, targetWorkspace, targetCredentials);

			JcrUtils.copy(sourceSession.getRootNode(), targetSession.getRootNode());
			return new SyncResult<Node>();
		} catch (URISyntaxException e) {
			throw new CommandArgsException(e);
		} catch (Exception e) {
			throw new CommandRuntimeException(e, this, t);
		}
	}

	protected Repository createRemoteRepository(URI uri) throws RepositoryException {
		RepositoryFactory repositoryFactory = new ClientDavexRepositoryFactory();
		Map<String, String> params = new HashMap<String, String>();
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_DAVEX_URI, uri.toString());
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, "main");
		return repositoryFactory.getRepository(params);
	}

	@Override
	public Options getOptions() {
		Options options = new Options();
		options.addOption(recursiveOption);
		options.addOption(deleteOption);
		options.addOption(progressOption);
		return options;
	}

	@Override
	public String getUsage() {
		return "[source URI] [target URI]";
	}

	public static void main(String[] args) {
		DescribedCommand.mainImpl(new JcrSync(), args);
	}

	@Override
	public String getDescription() {
		return "Synchronises JCR repositories";
	}

}
