package org.argeo.cms.cmd;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.fs.DavexFsProvider;
import org.argeo.util.LangUtils;

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

		try {
			URI sourceUri = new URI(arguments.get("-i"));
			URI targetUri = new URI(arguments.get("-o"));
			FileSystemProvider sourceFsProvider = createFsProvider(sourceUri);
			FileSystemProvider targetFsProvider = createFsProvider(targetUri);
			Path sourceBasePath = sourceFsProvider.getPath(sourceUri);
			Path targetBasePath = targetFsProvider.getPath(targetUri);
			SyncFileVisitor syncFileVisitor = new SyncFileVisitor(sourceBasePath, targetBasePath);
			ZonedDateTime begin = ZonedDateTime.now();
			Files.walkFileTree(sourceBasePath, syncFileVisitor);
			if (log.isDebugEnabled())
				log.debug("Sync from " + sourceBasePath + " to " + targetBasePath + " took " + LangUtils.since(begin));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static FileSystemProvider createFsProvider(URI uri) {
		FileSystemProvider fsProvider;
		if (uri.getScheme().equals("file"))
			fsProvider = FileSystems.getDefault().provider();
		else if (uri.getScheme().equals("davex"))
			fsProvider = new DavexFsProvider();
		else
			throw new CmsException("URI scheme not supported for " + uri);
		return fsProvider;
	}

	static enum Arg {
		to, from;
	}
}
