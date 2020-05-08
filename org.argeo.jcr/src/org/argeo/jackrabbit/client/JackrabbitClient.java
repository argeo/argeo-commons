package org.argeo.jackrabbit.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.RepositoryServiceImpl;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.argeo.jcr.JcrUtils;

/** Minimal client to test JCR DAVEX connectivity. */
public class JackrabbitClient {
	final static String JACKRABBIT_REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";
	final static String JACKRABBIT_DAVEX_URI = "org.apache.jackrabbit.spi2davex.uri";
	final static String JACKRABBIT_REMOTE_DEFAULT_WORKSPACE = "org.apache.jackrabbit.spi2davex.WorkspaceNameDefault";

	public static void main(String[] args) {
		String repoUri = args.length == 0 ? "http://root:demo@localhost:7070/jcr/ego" : args[0];
		String workspace = args.length < 2 ? "home" : args[1];

		Repository repository = null;
		Session session = null;

		URI uri;
		try {
			uri = new URI(repoUri);
		} catch (URISyntaxException e1) {
			throw new IllegalArgumentException(e1);
		}

		if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {

			RepositoryFactory repositoryFactory = new Jcr2davRepositoryFactory() {
				@SuppressWarnings("rawtypes")
				public Repository getRepository(Map parameters) throws RepositoryException {
					RepositoryServiceFactory repositoryServiceFactory = new Spi2davexRepositoryServiceFactory() {

						@Override
						public RepositoryService createRepositoryService(Map<?, ?> parameters)
								throws RepositoryException {
							Object uri = parameters.get(JACKRABBIT_DAVEX_URI);
							Object defaultWorkspace = parameters.get(JACKRABBIT_REMOTE_DEFAULT_WORKSPACE);
							BatchReadConfig brc = null;
							return new RepositoryServiceImpl(uri.toString(), defaultWorkspace.toString(), brc,
									ItemInfoCacheImpl.DEFAULT_CACHE_SIZE) {

								@Override
								protected HttpContext getContext(SessionInfo sessionInfo) throws RepositoryException {
									HttpClientContext result = HttpClientContext.create();
									result.setAuthCache(new NonSerialBasicAuthCache());
									return result;
								}

							};
						}
					};
					return RepositoryImpl.create(
							new Jcr2spiRepositoryFactory.RepositoryConfigImpl(repositoryServiceFactory, parameters));
				}
			};
			Map<String, String> params = new HashMap<String, String>();
			params.put(JACKRABBIT_DAVEX_URI, repoUri.toString());
			params.put(JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, "main");

			try {
				repository = repositoryFactory.getRepository(params);
				if (repository != null)
					session = repository.login(workspace);
				else
					throw new IllegalArgumentException("Repository " + repoUri + " not found");
			} catch (RepositoryException e) {
				e.printStackTrace();
			}

		} else {
			Path path = Paths.get(uri.getPath());
		}

		try {
			Node rootNode = session.getRootNode();
			NodeIterator nit = rootNode.getNodes();
			while (nit.hasNext()) {
				System.out.println(nit.nextNode().getPath());
			}

			Node newNode = JcrUtils.mkdirs(rootNode, "dir/subdir");
			System.out.println("Created folder " + newNode.getPath());
			Node newFile = JcrUtils.copyBytesAsFile(newNode, "test.txt", "TEST".getBytes());
			System.out.println("Created file " + newFile.getPath());
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(JcrUtils.getFileAsStream(newFile)))) {
				System.out.println("Read " + reader.readLine());
			} catch (IOException e) {
				e.printStackTrace();
			}
			newNode.getParent().remove();
			System.out.println("Removed new nodes");
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
}
