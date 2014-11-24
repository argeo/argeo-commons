package org.argeo.cms.text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.argeo.cms.CmsNames;
import org.argeo.cms.CmsUtils;
import org.argeo.cms.viewers.Section;
import org.eclipse.swt.widgets.Composite;

public class TextSection extends Section implements CmsNames {
	private static final long serialVersionUID = -8625209546243220689L;
	private String defaultTextStyle = TextStyles.TEXT_DEFAULT;
	private String titleStyle;

	public TextSection(Composite parent, int style, Node node)
			throws RepositoryException {
		this(parent, findSection(parent), style, node);
	}

	public TextSection(TextSection section, int style, Node node)
			throws RepositoryException {
		this(section, section.getParentSection(), style, node);
	}

	private TextSection(Composite parent, Section parentSection, int style,
			Node node) throws RepositoryException {
		super(parent, parentSection, style, node);
		CmsUtils.style(this, TextStyles.TEXT_SECTION);
	}

	public String getDefaultTextStyle() {
		return defaultTextStyle;
	}

	public String getTitleStyle() {
		if (titleStyle != null)
			return titleStyle;
		// TODO make base H styles configurable
		Integer relativeDepth = getRelativeDepth();
		return relativeDepth == 0 ? TextStyles.TEXT_TITLE : TextStyles.TEXT_H
				+ relativeDepth;
	}

	public void setDefaultTextStyle(String defaultTextStyle) {
		this.defaultTextStyle = defaultTextStyle;
	}

	public void setTitleStyle(String titleStyle) {
		this.titleStyle = titleStyle;
	}
}
