package org.argeo.cms.file.provider;

import java.nio.file.attribute.FileAttributeView;

import org.argeo.api.acr.Content;

public class ContentAttributeView implements FileAttributeView {
	final static String NAME = "content";

	private final Content content;

	public ContentAttributeView(Content content) {
		this.content = content;
	}

	@Override
	public String name() {
		return NAME;
	}

	public Content getContent() {
		return content;
	}
}
