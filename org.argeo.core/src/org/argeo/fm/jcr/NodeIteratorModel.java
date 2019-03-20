package org.argeo.fm.jcr;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

public class NodeIteratorModel implements TemplateSequenceModel {
	private final NodeIterator nodeIterator;

	private final List<Node> nodes;
	private int size;

	public NodeIteratorModel(NodeIterator nodeIterator) {
		super();
		this.nodeIterator = nodeIterator;
		this.size = (int) nodeIterator.getSize();
		this.nodes = new ArrayList<>(this.size);
		// TODO optimize with lazy loading
		while (this.nodeIterator.hasNext()) {
			Node node = this.nodeIterator.nextNode();
			nodes.add(node);
		}
		this.size = nodes.size();
	}

	@Override
	public TemplateModel get(int index) throws TemplateModelException {
		return new JcrModel(nodes.get(index));
	}

	@Override
	public int size() throws TemplateModelException {
		return size;
	}

}
