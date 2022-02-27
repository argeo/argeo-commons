package org.argeo.jcr.fs;

import java.nio.file.attribute.BasicFileAttributes;

import javax.jcr.Node;

public interface NodeFileAttributes extends BasicFileAttributes {
	public Node getNode();
}
