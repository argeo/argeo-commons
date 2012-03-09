/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.server.jcr;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jcr.AbstractInternalJackrabbitTestCase;
import org.argeo.jcr.JcrResourceAdapter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class JcrResourceAdapterTest extends AbstractInternalJackrabbitTestCase {
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMdd:hhmmss.SSS");

	private final static Log log = LogFactory
			.getLog(JcrResourceAdapterTest.class);

	private JcrResourceAdapter jra;

	public void testCreate() throws Exception {
		String basePath = "/test/subdir";
		jra.mkdirs(basePath);
		Resource res = new ClassPathResource("org/argeo/server/jcr/dummy00.xls");
		String filePath = basePath + "/dummy.xml";
		jra.create(filePath, res.getInputStream(), "application/vnd.ms-excel");
		InputStream in = jra.retrieve(filePath);
		assertTrue(IOUtils.contentEquals(res.getInputStream(), in));
	}

	public void testVersioning() throws Exception {
		String basePath = "/test/versions";
		jra.mkdirs(basePath);
		String filePath = basePath + "/dummy.xml";
		Resource res00 = new ClassPathResource(
				"org/argeo/server/jcr/dummy00.xls");
		jra.create(filePath, res00.getInputStream(), "application/vnd.ms-excel");
		Resource res01 = new ClassPathResource(
				"org/argeo/server/jcr/dummy01.xls");
		jra.update(filePath, res01.getInputStream());
		Resource res02 = new ClassPathResource(
				"org/argeo/server/jcr/dummy02.xls");
		jra.update(filePath, res02.getInputStream());

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
		jra.update(filePath, res03.getInputStream());
		in = jra.retrieve(filePath, 1);
		assertTrue(IOUtils.contentEquals(res01.getInputStream(), in));
	}

	@Override
	protected void setUp() throws Exception {
		log.debug("SET UP");
		super.setUp();
		jra = new JcrResourceAdapter();
		jra.setSession(session());
	}

	@Override
	protected void tearDown() throws Exception {
		log.debug("TEAR DOWN");
		super.tearDown();
	}
}
