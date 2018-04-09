package org.argeo.cms.cmd;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.cms.internal.kernel.KernelConstants;
import org.argeo.jackrabbit.fs.DavexFsProvider;
import org.argeo.jcr.ArgeoJcrException;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.fs.JcrFileSystem;

public class Sync {
	private final static Log log = LogFactory.getLog(Sync.class);

	public static void main(String args[]) {
		Map<String, String> arguments = new HashMap<>();
		boolean skipNext = false;
		String currValue = null;
		for (int i = 0; i < args.length; i++) {
			if (skipNext) {
				skipNext = false;
				currValue = null;
				continue;
			}
			String arg = args[i];
			if (arg.startsWith("-")) {
				if (i + 1 < args.length) {
					if (!args[i + 1].startsWith("-")) {
						currValue = args[i + 1];
						skipNext = true;
					}
				}
				arguments.put(arg, currValue);
			} else {
				// TODO add multiple?
			}
		}

		log.debug("Hello world " + arguments);
		String sourceUri = arguments.get("-i");
		DavexFsProvider fsProvider = new DavexFsProvider();
//		Map<String, String> params = new HashMap<String, String>();
//		params.put(KernelConstants.JACKRABBIT_REPOSITORY_URI, sourceUri);
//		params.put(KernelConstants.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, "main");
//		Repository repository;
//		try {
//			repository = new Jcr2davRepositoryFactory().getRepository(params);
//			if (repository == null)
//				throw new ArgeoJcrException("Remote Davex repository " + sourceUri + " not found");
//			Session session = repository.login();
//			if (log.isDebugEnabled())
//				log.debug("Opened JCR session to " + sourceUri);
//			JcrUtils.logoutQuietly(session);
//		} catch (RepositoryException e) {
//			throw new ArgeoJcrException("Cannot load " + sourceUri, e);
//		}

	}

	static enum Arg {
		to, from;
	}
}
