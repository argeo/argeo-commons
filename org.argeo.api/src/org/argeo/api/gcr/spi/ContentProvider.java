package org.argeo.api.gcr.spi;

import java.util.function.Supplier;

import org.argeo.api.gcr.Content;

public interface ContentProvider extends Supplier<Content> {

	Content get(String relativePath);

}
