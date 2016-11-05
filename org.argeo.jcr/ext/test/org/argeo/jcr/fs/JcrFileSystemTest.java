package org.argeo.jcr.fs;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jackrabbit.fs.JackrabbitMemoryFsProvider;

import junit.framework.TestCase;

public class JcrFileSystemTest extends TestCase {
	private final static Log log = LogFactory.getLog(JcrFileSystemTest.class);

	public void testSimple() throws Exception {
		FileSystemProvider fsProvider = new JackrabbitMemoryFsProvider();

		// Simple file
		Path testPath = fsProvider.getPath(new URI("jcr+memory:/test.txt"));
		Files.createFile(testPath);
		BasicFileAttributes bfa = Files.readAttributes(testPath, BasicFileAttributes.class);
		FileTime ft = bfa.creationTime();
		assertNotNull(ft);
		assertTrue(bfa.isRegularFile());
		log.debug("Created " + testPath + " (" + ft + ")");
		Files.delete(testPath);
		log.debug("Deleted " + testPath);
		String txt = "TEST\nTEST2\n";
		byte[] arr = txt.getBytes();
		Files.write(testPath, arr);
		log.debug("Wrote " + testPath);
		byte[] read = Files.readAllBytes(testPath);
		assertTrue(Arrays.equals(arr, read));
		assertEquals(txt, new String(read));
		log.debug("Read " + testPath);
		Path rootPath = fsProvider.getPath(new URI("jcr+memory:/"));
		log.debug("Got root " + rootPath);
		Path testDir = rootPath.resolve("testDir");
		log.debug("Resolved " + testDir);
		// Copy
		Files.createDirectory(testDir);
		log.debug("Created directory " + testDir);
		Path subsubdir = Files.createDirectories(testDir.resolve("subdir/subsubdir"));
		log.debug("Created sub directories " + subsubdir);
		Path copiedFile = testDir.resolve("copiedFile.txt");
		log.debug("Resolved " + copiedFile);
		try (OutputStream out = Files.newOutputStream(copiedFile); InputStream in = Files.newInputStream(testPath)) {
			IOUtils.copy(in, out);
		}
		log.debug("Copied " + testPath + " to " + copiedFile);
		Files.delete(testPath);
		log.debug("Deleted " + testPath);
		byte[] copiedRead = Files.readAllBytes(copiedFile);
		assertTrue(Arrays.equals(copiedRead, read));
		log.debug("Read " + copiedFile);
		// Browse directories
		DirectoryStream<Path> files = Files.newDirectoryStream(testDir);
		int fileCount = 0;
		Path listedFile = null;
		for (Path file : files) {
			fileCount++;
			if (!Files.isDirectory(file))
				listedFile = file;
		}
		assertEquals(2, fileCount);
		assertEquals(copiedFile, listedFile);
		assertEquals(copiedFile.toString(), listedFile.toString());
		log.debug("Listed " + testDir);
		// Generic attributes
		Map<String, Object> attrs = Files.readAttributes(copiedFile, "*");
		assertEquals(5, attrs.size());
		log.debug("Read attributes of " + copiedFile + ": " + attrs.keySet());
		// Direct node access
		NodeFileAttributes nfa = Files.readAttributes(copiedFile, NodeFileAttributes.class);
		nfa.getNode().addMixin(NodeType.MIX_LANGUAGE);
		nfa.getNode().getSession().save();
		log.debug("Add mix:language");
		Files.setAttribute(copiedFile, Property.JCR_LANGUAGE, "fr");
		log.debug("Set language");
		attrs = Files.readAttributes(copiedFile, "*");
		assertEquals(6, attrs.size());
		log.debug("Read attributes of " + copiedFile + ": " + attrs.keySet());
	}
}
