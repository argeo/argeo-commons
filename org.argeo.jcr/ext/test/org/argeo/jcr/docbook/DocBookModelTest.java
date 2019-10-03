package org.argeo.jcr.docbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.argeo.jackrabbit.unit.AbstractJackrabbitTestCase;
import org.argeo.jcr.JcrUtils;

public class DocBookModelTest extends AbstractJackrabbitTestCase {
	private final static Log log = LogFactory.getLog(DocBookModelTest.class);

	public void testLoadWikipediaSample() throws Exception {
		importXml("WikipediaSample.dbk.xml");
	}

	public void XXXtestLoadHowTo() throws Exception {
		importXml("howto.xml", false);
	}

	protected void importXml(String res) throws Exception {
		importXml(res, true);
	}

	protected void importXml(String res, Boolean mini) throws Exception {
		byte[] bytes;
		try (InputStream in = getClass().getResourceAsStream(res)) {
			bytes = IOUtils.toByteArray(in);
		}

		{// cnd
			long begin = System.currentTimeMillis();
			if (mini) {
				InputStreamReader reader = new InputStreamReader(getClass()
						.getResourceAsStream(
								"/org/argeo/jcr/docbook/docbook.cnd"));
				CndImporter.registerNodeTypes(reader, session());
				reader.close();
			} else {
				InputStreamReader reader = new InputStreamReader(getClass()
						.getResourceAsStream(
								"/org/argeo/jcr/docbook/docbook-full.cnd"));
				CndImporter.registerNodeTypes(reader, session());
				reader.close();
			}
			long duration = System.currentTimeMillis() - begin;
			if (log.isDebugEnabled())
				log.debug(" CND loaded in " + duration + " ms");
		}

		String testPath = "/" + res;
		// if (mini)
		JcrUtils.mkdirs(session(), testPath, "dbk:set");
		// else
		// JcrUtils.mkdirs(session(), testPath, "dbk:book");

		DocBookModel model = new DocBookModel(session());
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			long begin = System.currentTimeMillis();
			model.importXml(testPath, in);
			long duration = System.currentTimeMillis() - begin;
			if (log.isDebugEnabled())
				log.debug("Imported " + res + " " + (bytes.length / 1024l)
						+ " kB in " + duration + " ms ("
						+ (bytes.length / duration) + " B/ms)");
		}

		saveSession();
		// JcrUtils.debug(session().getRootNode());

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try {
				model.exportXml(testPath + "/dbk:book", out);
			} catch (Exception e) {
				model.exportXml(testPath + "/dbk:article", out);
			}
			bytes = out.toByteArray();

			session().logout();
			model.setSession(session());

			// log.debug(new String(bytes));
			try (InputStream in = new ByteArrayInputStream(bytes)) {
				long begin = System.currentTimeMillis();
				model.importXml(testPath, in);
				long duration = System.currentTimeMillis() - begin;
				if (log.isDebugEnabled())
					log.debug("Re-imported " + res + " "
							+ (bytes.length / 1024l) + " kB in " + duration
							+ " ms (" + (bytes.length / duration) + " B/ms)");
			}
		}
		saveSession();
	}

	protected void saveSession() throws RepositoryException {
		long begin = System.currentTimeMillis();
		session().save();
		long duration = System.currentTimeMillis() - begin;
		if (log.isDebugEnabled())
			log.debug(" Session save took " + duration + " ms");
	}

	// public static Test suite() {
	// return defaultTestSuite(DocBookModelTest.class);
	// }

}
