package org.argeo.server.jcr;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class JcrResourceAdapterTest extends TestCase {
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMdd:hhmmss.SSS");

	private final static Log log = LogFactory
			.getLog(JcrResourceAdapterTest.class);

	private JcrResourceAdapter jra;
	private TransientRepository repository;

	public void testCreate() throws Exception {
		String basePath = "/test/subdir";
		jra.mkdirs(basePath);
		Resource res = new ClassPathResource("org/argeo/server/jcr/dummy00.xls");
		String filePath = basePath + "/dummy.xml";
		jra.create(filePath, res, "application/vnd.ms-excel");
		InputStream in = jra.retrieve(filePath);
		assertTrue(IOUtils.contentEquals(res.getInputStream(), in));
	}

	public void testVersioning() throws Exception {
		String basePath = "/test/versions";
		jra.mkdirs(basePath);
		String filePath = basePath + "/dummy.xml";
		Resource res00 = new ClassPathResource(
				"org/argeo/server/jcr/dummy00.xls");
		jra.create(filePath, res00, "application/vnd.ms-excel");
		Resource res01 = new ClassPathResource(
				"org/argeo/server/jcr/dummy01.xls");
		jra.update(filePath, res01);
		Resource res02 = new ClassPathResource(
				"org/argeo/server/jcr/dummy02.xls");
		jra.update(filePath, res02);

		List<Calendar> versions = jra.listVersions(filePath);
		log.debug("Versions of " + filePath);
		int count = 0;
		for (Calendar version : versions) {
			log.debug(" " + (count == 0 ? "base" : count - 1) + "\t"
					+ sdf.format(version.getTime()));
			count++;
		}

		assertEquals(4, versions.size());

		InputStream in = jra.retrieve(filePath, 1);
		assertTrue(IOUtils.contentEquals(res01.getInputStream(), in));
		in = jra.retrieve(filePath, 0);
		assertTrue(IOUtils.contentEquals(res00.getInputStream(), in));
		in = jra.retrieve(filePath, 2);
		assertTrue(IOUtils.contentEquals(res02.getInputStream(), in));
		Resource res03 = new ClassPathResource(
				"org/argeo/server/jcr/dummy03.xls");
		jra.update(filePath, res03);
		in = jra.retrieve(filePath, 1);
		assertTrue(IOUtils.contentEquals(res01.getInputStream(), in));
	}

	@Override
	protected void setUp() throws Exception {
		File homeDir = new File(System.getProperty("java.io.tmpdir"),
				JcrResourceAdapterTest.class.getSimpleName());
		FileUtils.deleteDirectory(homeDir);
		Resource res = new ClassPathResource(
				"org/argeo/server/jcr/repository.xml");
		repository = new TransientRepository(res.getFile(), homeDir);

		jra = new JcrResourceAdapter();
		jra.setRepository(repository);
		jra.setUsername("demo");
		jra.setPassword("demo");
		jra.afterPropertiesSet();
	}

	@Override
	protected void tearDown() throws Exception {
		jra.destroy();
		// repository.shutdown();
	}

}
