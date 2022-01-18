package org.argeo.cms.gcr.xml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentUtils;
import org.argeo.api.gcr.spi.ContentProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DomContentProvider implements ContentProvider {
	private Document document;

	public DomContentProvider(Document document) {
		this.document = document;
		this.document.normalizeDocument();
	}

	@Override
	public Content get() {
		return new DomContent(this, document.getDocumentElement());
	}

	public Element createElement(String name) {
		return document.createElement(name);
	}

	@Override
	public Content get(String relativePath) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String args[]) throws Exception {
		HashMap<String, Object> map = new HashMap<>();
		map.put(null, "test");
		System.out.println(map.get(null));

		Set<String> set = new HashSet<>();
		set.add(null);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Path testFile;
		testFile = Paths.get(System.getProperty("user.home") + "/dev/git/unstable/argeo-commons/pom.xml");
		testFile = Paths.get(System.getProperty("user.home") + "/tmp/test.xml");
		Document doc = dBuilder.parse(Files.newInputStream(testFile));

		DomContentProvider contentSession = new DomContentProvider(doc);
		ContentUtils.traverse(contentSession.get(), (c, d) -> ContentUtils.print(c, System.out, d, true));

	}
}
